package com.edu.api.pop_cube_wallet.wallet.application;

import com.edu.api.pop_cube_wallet.wallet.WalletApi;
import com.edu.api.pop_cube_wallet.wallet.domain.LedgerEntry;
import com.edu.api.pop_cube_wallet.wallet.domain.LedgerEntryRepository;
import com.edu.api.pop_cube_wallet.wallet.domain.Wallet;
import com.edu.api.pop_cube_wallet.wallet.domain.WalletRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Handles wallet write operations and delegates reads to WalletQueryHandler.
 * Implements WalletApi so other modules (Transaction) can call debit/credit/getBalance synchronously.
 */
@Service
class WalletCommandHandler implements WalletApi {

    private static final Logger log = LoggerFactory.getLogger(WalletCommandHandler.class);

    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final WalletQueryHandler walletQueryHandler;
    private final Counter ledgerCreditCounter;
    private final Counter ledgerDebitCounter;
    private final Timer balanceQueryTimer;

    WalletCommandHandler(WalletRepository walletRepository,
                         LedgerEntryRepository ledgerEntryRepository,
                         WalletQueryHandler walletQueryHandler,
                         MeterRegistry meterRegistry) {
        this.walletRepository = walletRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.walletQueryHandler = walletQueryHandler;
        this.ledgerCreditCounter = Counter.builder("wallet.ledger.entries.count")
                .tag("type", "CREDIT")
                .description("Number of ledger entries created")
                .register(meterRegistry);
        this.ledgerDebitCounter = Counter.builder("wallet.ledger.entries.count")
                .tag("type", "DEBIT")
                .description("Number of ledger entries created")
                .register(meterRegistry);
        this.balanceQueryTimer = Timer.builder("wallet.balance.query.duration")
                .description("Time taken to query wallet balance")
                .register(meterRegistry);
    }

    @Override
    @Transactional
    public void debit(UUID accountId, BigDecimal amount, UUID transactionId, String description) {
        Wallet wallet = walletRepository.findByAccountId(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Wallet not found for account: " + accountId));

        LedgerEntry entry = wallet.debit(amount, transactionId, description);
        walletRepository.save(wallet);
        ledgerEntryRepository.save(entry);

        log.info("Wallet debited: accountId={}, amount={}, txId={}", accountId, amount, transactionId);
        ledgerDebitCounter.increment();
    }

    @Override
    @Transactional
    public void credit(UUID accountId, BigDecimal amount, UUID transactionId, String description) {
        Wallet wallet = walletRepository.findByAccountId(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Wallet not found for account: " + accountId));

        LedgerEntry entry = wallet.credit(amount, transactionId, description);
        walletRepository.save(wallet);
        ledgerEntryRepository.save(entry);

        log.info("Wallet credited: accountId={}, amount={}, txId={}", accountId, amount, transactionId);
        ledgerCreditCounter.increment();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getBalance(UUID accountId) {
        return balanceQueryTimer.record(() -> walletQueryHandler.execute(new GetBalanceQuery(accountId)));
    }
}
