package com.edu.api.pop_cube_wallet.audit.application;

import com.edu.api.pop_cube_wallet.audit.domain.AuditEntry;
import com.edu.api.pop_cube_wallet.audit.domain.AuditEntryRepository;
import com.edu.api.pop_cube_wallet.transaction.TransactionCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Listens to TransactionCompletedEvent and creates immutable audit trail entries.
 * Uses @ApplicationModuleListener for at-least-once delivery via Event Publication Registry.
 * Entries are generated only after a successful transaction commit (ADR-0006).
 * Handler is idempotent — duplicate events produce no additional entries.
 */
@Component
@RequiredArgsConstructor
class AuditEventHandler {

    private static final Logger log = LoggerFactory.getLogger(AuditEventHandler.class);

    private final AuditEntryRepository auditEntryRepository;

    @ApplicationModuleListener
    void onTransactionCompleted(TransactionCompletedEvent event) {
        // Audit entry for the source account (DEBIT side)
        persistIfAbsent(
                event.sourceAccountId(),
                "P2P_TRANSFER_SENT",
                "Transfer of %s to account %s (tx=%s)".formatted(
                        event.amount(), event.destinationAccountId(), event.transactionId()),
                event.completedAt()
        );

        // Audit entry for the destination account (CREDIT side)
        persistIfAbsent(
                event.destinationAccountId(),
                "P2P_TRANSFER_RECEIVED",
                "Received %s from account %s (tx=%s)".formatted(
                        event.amount(), event.sourceAccountId(), event.transactionId()),
                event.completedAt()
        );

        log.info("Audit entries recorded for tx={}", event.transactionId());
    }

    /**
     * Idempotency guard: skips persistence if an entry with the same account, event type,
     * and timestamp already exists (indicates a replayed event).
     */
    private void persistIfAbsent(java.util.UUID accountId, String eventType,
                                  String description, java.time.LocalDateTime occurredAt) {
        if (auditEntryRepository.existsByAccountIdAndEventTypeAndOccurredAt(
                accountId, eventType, occurredAt)) {
            log.warn("Audit entry already exists for account={}, type={}, at={} — skipping",
                    accountId, eventType, occurredAt);
            return;
        }

        AuditEntry entry = AuditEntry.create(accountId, eventType, description);
        auditEntryRepository.save(entry);
    }
}
