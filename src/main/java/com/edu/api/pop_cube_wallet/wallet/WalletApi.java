package com.edu.api.pop_cube_wallet.wallet;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Public API of the Wallet module, consumable by other bounded contexts (e.g., Transaction).
 * Synchronous calls are allowed per ADR-0008 when an immediate return value is required.
 */
public interface WalletApi {

    /**
     * Returns the current available balance for the given account.
     */
    BigDecimal getBalance(UUID accountId);

    /**
     * Debit the wallet of the given account. Participates in the caller's transaction.
     *
     * @throws IllegalStateException if insufficient balance
     */
    void debit(UUID accountId, BigDecimal amount, UUID transactionId, String description);

    /**
     * Credit the wallet of the given account. Participates in the caller's transaction.
     */
    void credit(UUID accountId, BigDecimal amount, UUID transactionId, String description);
}
