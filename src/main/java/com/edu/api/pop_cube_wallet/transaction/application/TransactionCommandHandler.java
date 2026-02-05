package com.edu.api.pop_cube_wallet.transaction.application;

import com.edu.api.pop_cube_wallet.account.AccountApi;
import com.edu.api.pop_cube_wallet.transaction.TransactionCompletedEvent;
import com.edu.api.pop_cube_wallet.transaction.domain.Transaction;
import com.edu.api.pop_cube_wallet.transaction.domain.TransactionRepository;
import com.edu.api.pop_cube_wallet.wallet.WalletApi;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Command handler that orchestrates the P2P transfer flow.
 * Validates accounts, debits/credits wallets, persists transaction, and publishes event.
 * All steps run in a single transaction for atomicity (ADR-0006).
 */
@Service
class TransactionCommandHandler implements TransferUseCase {

    private static final Logger log = LoggerFactory.getLogger(TransactionCommandHandler.class);

    private final AccountApi accountApi;
    private final WalletApi walletApi;
    private final TransactionRepository transactionRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Counter successCounter;
    private final Counter failureInsufficientFundsCounter;
    private final Counter failureDuplicateCounter;
    private final Timer transactionTimer;
    private final DistributionSummary transferAmountSummary;

    TransactionCommandHandler(AccountApi accountApi,
                              WalletApi walletApi,
                              TransactionRepository transactionRepository,
                              ApplicationEventPublisher eventPublisher,
                              MeterRegistry meterRegistry) {
        this.accountApi = accountApi;
        this.walletApi = walletApi;
        this.transactionRepository = transactionRepository;
        this.eventPublisher = eventPublisher;
        this.successCounter = Counter.builder("wallet.transaction.success.count")
                .tag("type", "P2P")
                .description("Number of successful P2P transactions")
                .register(meterRegistry);
        this.failureInsufficientFundsCounter = Counter.builder("wallet.transaction.failure.count")
                .tag("reason", "insufficient_funds")
                .description("Transactions failed due to insufficient funds")
                .register(meterRegistry);
        this.failureDuplicateCounter = Counter.builder("wallet.transaction.failure.count")
                .tag("reason", "duplicate")
                .description("Duplicate idempotency key rejections")
                .register(meterRegistry);
        this.transactionTimer = Timer.builder("wallet.transaction.duration")
                .description("Time taken to execute a P2P transfer")
                .register(meterRegistry);
        this.transferAmountSummary = DistributionSummary.builder("wallet.transfer.amount.sum")
                .baseUnit("BRL")
                .description("Distribution of P2P transfer amounts")
                .register(meterRegistry);
    }

    @Override
    @Transactional
    public TransferResult execute(TransferCommand command) {
        return transactionTimer.record(() -> doExecute(command));
    }

    private TransferResult doExecute(TransferCommand command) {
        // 1. Idempotency check: if a transaction with this key already exists, return it
        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(command.idempotencyKey());
        if (existing.isPresent()) {
            log.info("Idempotent duplicate detected: key={}", command.idempotencyKey());
            failureDuplicateCounter.increment();
            return toResult(existing.get());
        }

        // 2. Validate source account exists and is ACTIVE.
        //    BLOCKED/PENDING_KYC accounts cannot initiate debits (send money).
        //    Uses existsAndActive() for a single-purpose check; on failure, falls back
        //    to exists() to distinguish "not found" (404) from "not active" (state error).
        if (!accountApi.existsAndActive(command.sourceAccountId())) {
            if (!accountApi.exists(command.sourceAccountId())) {
                throw new EntityNotFoundException(
                        "Source account not found: " + command.sourceAccountId());
            }
            throw new IllegalStateException(
                    "Source account is not active (debits are forbidden): " + command.sourceAccountId());
        }

        // 3. Validate destination account exists (any status).
        //    BLOCKED accounts CAN still receive credits — only debits are forbidden.
        if (!accountApi.exists(command.destinationAccountId())) {
            throw new EntityNotFoundException(
                    "Destination account not found: " + command.destinationAccountId());
        }

        // 4. Create domain aggregate (validates business rules: same-account, positive amount, etc.)
        Transaction transaction = Transaction.create(
                command.sourceAccountId(),
                command.destinationAccountId(),
                command.amount(),
                command.description(),
                command.idempotencyKey()
        );

        // 5. Deadlock prevention: always lock wallets in deterministic UUID order.
        //    Without this, concurrent transfers A→B and B→A could each lock one wallet
        //    row first, then wait for the other — classic ABBA deadlock.
        //    Sorting by UUID natural order guarantees both transactions acquire the
        //    lower-UUID wallet lock first, eliminating the circular wait condition.
        UUID sourceId = command.sourceAccountId();
        UUID destId = command.destinationAccountId();
        boolean sourceFirst = sourceId.compareTo(destId) < 0;

        UUID firstAccount = sourceFirst ? sourceId : destId;
        UUID secondAccount = sourceFirst ? destId : sourceId;

        // Lock first wallet (lower UUID)
        if (sourceFirst) {
            try {
                walletApi.debit(firstAccount, command.amount(), transaction.getId(),
                        "P2P transfer to " + destId);
            } catch (IllegalStateException ex) {
                failureInsufficientFundsCounter.increment();
                throw ex;
            }
            walletApi.credit(secondAccount, command.amount(), transaction.getId(),
                    "P2P transfer from " + sourceId);
        } else {
            // Destination has the lower UUID — credit it first to acquire its lock,
            // then debit the source (higher UUID).
            walletApi.credit(firstAccount, command.amount(), transaction.getId(),
                    "P2P transfer from " + sourceId);
            try {
                walletApi.debit(secondAccount, command.amount(), transaction.getId(),
                        "P2P transfer to " + destId);
            } catch (IllegalStateException ex) {
                failureInsufficientFundsCounter.increment();
                throw ex;
            }
        }

        // 7. Persist the transaction record
        Transaction saved = transactionRepository.save(transaction);

        log.info("P2P transfer completed: txId={}, from={}, to={}, amount={}",
                saved.getId(), saved.getSourceAccountId(),
                saved.getDestinationAccountId(), saved.getAmount());

        successCounter.increment();
        transferAmountSummary.record(saved.getAmount().doubleValue());

        // 8. Publish event (processed after commit by @ApplicationModuleListener handlers)
        eventPublisher.publishEvent(new TransactionCompletedEvent(
                saved.getId(),
                saved.getSourceAccountId(),
                saved.getDestinationAccountId(),
                saved.getAmount(),
                saved.getDescription(),
                saved.getCreatedAt()
        ));

        return toResult(saved);
    }

    private TransferResult toResult(Transaction tx) {
        return new TransferResult(
                tx.getId(),
                tx.getSourceAccountId(),
                tx.getDestinationAccountId(),
                tx.getAmount(),
                tx.getStatus().name(),
                tx.getCreatedAt()
        );
    }
}
