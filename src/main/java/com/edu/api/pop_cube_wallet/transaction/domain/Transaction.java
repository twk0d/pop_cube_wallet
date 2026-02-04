package com.edu.api.pop_cube_wallet.transaction.domain;

import lombok.Getter;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Transaction aggregate root — the "Engine" bounded context.
 * Represents a completed P2P money movement.
 */
@AggregateRoot
@Getter
public class Transaction {

    @Identity
    private UUID id;
    private UUID sourceAccountId;
    private UUID destinationAccountId;
    private BigDecimal amount;
    private String description;
    private TransactionStatus status;
    private String idempotencyKey;
    private LocalDateTime createdAt;

    private Transaction() {}

    /**
     * Factory: creates a new successful P2P transaction.
     */
    public static Transaction create(UUID sourceAccountId, UUID destinationAccountId,
                                     BigDecimal amount, String description,
                                     String idempotencyKey) {
        if (sourceAccountId.equals(destinationAccountId)) {
            throw new IllegalArgumentException("Source and destination accounts must be different");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency key is required");
        }

        Transaction tx = new Transaction();
        tx.id = UUID.randomUUID();
        tx.sourceAccountId = sourceAccountId;
        tx.destinationAccountId = destinationAccountId;
        tx.amount = amount;
        tx.description = description;
        tx.status = TransactionStatus.COMPLETED;
        tx.idempotencyKey = idempotencyKey;
        tx.createdAt = LocalDateTime.now(ZoneOffset.UTC);
        return tx;
    }

    /**
     * Reconstruct from persistence.
     */
    public static Transaction reconstitute(UUID id, UUID sourceAccountId, UUID destinationAccountId,
                                            BigDecimal amount, String description,
                                            TransactionStatus status, String idempotencyKey,
                                            LocalDateTime createdAt) {
        Transaction tx = new Transaction();
        tx.id = id;
        tx.sourceAccountId = sourceAccountId;
        tx.destinationAccountId = destinationAccountId;
        tx.amount = amount;
        tx.description = description;
        tx.status = status;
        tx.idempotencyKey = idempotencyKey;
        tx.createdAt = createdAt;
        return tx;
    }
}
