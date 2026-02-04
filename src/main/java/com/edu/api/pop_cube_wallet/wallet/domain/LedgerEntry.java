package com.edu.api.pop_cube_wallet.wallet.domain;

import lombok.Getter;
import org.jmolecules.ddd.annotation.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Immutable ledger entry representing a single credit or debit movement.
 * Part of the Wallet aggregate — ensures double-entry integrity.
 */
@Entity
@Getter
public class LedgerEntry {

    private UUID id;
    private UUID walletId;
    private LedgerEntryType entryType;
    private BigDecimal amount;
    private UUID transactionId;
    private String description;
    private LocalDateTime createdAt;

    private LedgerEntry() {}

    public static LedgerEntry create(UUID walletId, LedgerEntryType entryType,
                                     BigDecimal amount, UUID transactionId, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Ledger entry amount must be positive");
        }

        LedgerEntry entry = new LedgerEntry();
        entry.id = UUID.randomUUID();
        entry.walletId = walletId;
        entry.entryType = entryType;
        entry.amount = amount;
        entry.transactionId = transactionId;
        entry.description = description;
        entry.createdAt = LocalDateTime.now(ZoneOffset.UTC);
        return entry;
    }

    public static LedgerEntry reconstitute(UUID id, UUID walletId, LedgerEntryType entryType,
                                            BigDecimal amount, UUID transactionId,
                                            String description, LocalDateTime createdAt) {
        LedgerEntry entry = new LedgerEntry();
        entry.id = id;
        entry.walletId = walletId;
        entry.entryType = entryType;
        entry.amount = amount;
        entry.transactionId = transactionId;
        entry.description = description;
        entry.createdAt = createdAt;
        return entry;
    }
}
