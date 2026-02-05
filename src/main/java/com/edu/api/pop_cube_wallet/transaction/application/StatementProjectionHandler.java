package com.edu.api.pop_cube_wallet.transaction.application;

import com.edu.api.pop_cube_wallet.transaction.TransactionCompletedEvent;
import com.edu.api.pop_cube_wallet.transaction.domain.StatementEntry;
import com.edu.api.pop_cube_wallet.transaction.domain.StatementRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Asynchronous projection handler that materializes the denormalized statement read model
 * from TransactionCompletedEvent (ADR-0006 CQRS strategy).
 * Creates two entries per transaction: one SENT (sender) and one RECEIVED (receiver).
 * Handler is idempotent — duplicate events are safely skipped.
 */
@Component
@RequiredArgsConstructor
class StatementProjectionHandler {

    private static final Logger log = LoggerFactory.getLogger(StatementProjectionHandler.class);

    private final StatementRepository statementRepository;

    @ApplicationModuleListener
    void onTransactionCompleted(TransactionCompletedEvent event) {
        // Idempotency: skip if entries already exist for this transaction + account
        if (statementRepository.existsByTransactionIdAndAccountId(
                event.transactionId(), event.sourceAccountId())) {
            log.warn("Statement entries already exist for tx={}, skipping", event.transactionId());
            return;
        }

        // SENT entry for the source account
        StatementEntry sentEntry = StatementEntry.sent(
                event.transactionId(),
                event.sourceAccountId(),
                event.destinationAccountId(),
                event.amount(),
                event.description(),
                event.completedAt()
        );
        statementRepository.save(sentEntry);

        // RECEIVED entry for the destination account
        StatementEntry receivedEntry = StatementEntry.received(
                event.transactionId(),
                event.destinationAccountId(),
                event.sourceAccountId(),
                event.amount(),
                event.description(),
                event.completedAt()
        );
        statementRepository.save(receivedEntry);

        log.info("Statement projected for tx={}", event.transactionId());
    }
}
