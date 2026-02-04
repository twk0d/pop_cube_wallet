package com.edu.api.pop_cube_wallet.wallet.domain;

import lombok.Getter;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Wallet aggregate root — the "Vault" bounded context.
 * Manages balance via ledger entries; no direct balance mutation from outside.
 * Uses optimistic locking (version) to prevent concurrent balance conflicts (ADR risk mitigation).
 */
@AggregateRoot
@Getter
public class Wallet {

    @Identity
    private UUID id;
    private UUID accountId;
    private BigDecimal balance;
    private long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Wallet() {}

    /**
     * Factory: creates a new wallet with zero balance for the given account.
     */
    public static Wallet create(UUID accountId) {
        Wallet wallet = new Wallet();
        wallet.id = UUID.randomUUID();
        wallet.accountId = accountId;
        wallet.balance = BigDecimal.ZERO;
        wallet.version = 0L;
        wallet.createdAt = LocalDateTime.now(ZoneOffset.UTC);
        wallet.updatedAt = LocalDateTime.now(ZoneOffset.UTC);
        return wallet;
    }

    /**
     * Reconstruct from persistence.
     */
    public static Wallet reconstitute(UUID id, UUID accountId, BigDecimal balance,
                                       long version, LocalDateTime createdAt,
                                       LocalDateTime updatedAt) {
        Wallet wallet = new Wallet();
        wallet.id = id;
        wallet.accountId = accountId;
        wallet.balance = balance;
        wallet.version = version;
        wallet.createdAt = createdAt;
        wallet.updatedAt = updatedAt;
        return wallet;
    }

    /**
     * Apply a credit (increase balance).
     */
    public LedgerEntry credit(BigDecimal amount, UUID transactionId, String description) {
        validatePositiveAmount(amount);
        this.balance = this.balance.add(amount);
        this.updatedAt = LocalDateTime.now(ZoneOffset.UTC);
        return LedgerEntry.create(this.id, LedgerEntryType.CREDIT, amount, transactionId, description);
    }

    /**
     * Apply a debit (decrease balance). Throws if insufficient funds.
     */
    public LedgerEntry debit(BigDecimal amount, UUID transactionId, String description) {
        validatePositiveAmount(amount);
        if (this.balance.compareTo(amount) < 0) {
            throw new IllegalStateException(
                    "Insufficient balance: available=" + this.balance + ", requested=" + amount
            );
        }
        this.balance = this.balance.subtract(amount);
        this.updatedAt = LocalDateTime.now(ZoneOffset.UTC);
        return LedgerEntry.create(this.id, LedgerEntryType.DEBIT, amount, transactionId, description);
    }

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }
}
