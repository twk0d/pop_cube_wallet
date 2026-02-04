package com.edu.api.pop_cube_wallet.transaction.domain;

import lombok.Getter;
import org.jmolecules.architecture.cqrs.QueryModel;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * CQRS Read Model: denormalized statement entry for fast account history queries (ADR-0006).
 * Populated asynchronously from TransactionCompletedEvent.
 */
@QueryModel
@Getter
public class StatementEntry {

    private UUID id;
    private UUID accountId;
    private UUID transactionId;
    private String entryType; // SENT or RECEIVED
    private BigDecimal amount;
    private UUID counterpartyAccountId;
    private String description;
    private LocalDateTime createdAt;

    private StatementEntry() {}

    /**
     * Factory for SENT entry (from the sender's perspective).
     */
    public static StatementEntry sent(UUID transactionId, UUID senderAccountId,
                                      UUID receiverAccountId, BigDecimal amount,
                                      String description, LocalDateTime timestamp) {
        StatementEntry e = new StatementEntry();
        e.id = UUID.randomUUID();
        e.accountId = senderAccountId;
        e.transactionId = transactionId;
        e.entryType = "SENT";
        e.amount = amount;
        e.counterpartyAccountId = receiverAccountId;
        e.description = description;
        e.createdAt = timestamp;
        return e;
    }

    /**
     * Factory for RECEIVED entry (from the receiver's perspective).
     */
    public static StatementEntry received(UUID transactionId, UUID receiverAccountId,
                                          UUID senderAccountId, BigDecimal amount,
                                          String description, LocalDateTime timestamp) {
        StatementEntry e = new StatementEntry();
        e.id = UUID.randomUUID();
        e.accountId = receiverAccountId;
        e.transactionId = transactionId;
        e.entryType = "RECEIVED";
        e.amount = amount;
        e.counterpartyAccountId = senderAccountId;
        e.description = description;
        e.createdAt = timestamp;
        return e;
    }

    /**
     * Reconstruct from persistence.
     */
    public static StatementEntry reconstitute(UUID id, UUID accountId, UUID transactionId,
                                               String entryType, BigDecimal amount,
                                               UUID counterpartyAccountId, String description,
                                               LocalDateTime createdAt) {
        StatementEntry e = new StatementEntry();
        e.id = id;
        e.accountId = accountId;
        e.transactionId = transactionId;
        e.entryType = entryType;
        e.amount = amount;
        e.counterpartyAccountId = counterpartyAccountId;
        e.description = description;
        e.createdAt = createdAt;
        return e;
    }
}
