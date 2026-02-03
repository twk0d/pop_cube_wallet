package com.edu.api.pop_cube_wallet.account;

import java.util.Optional;
import java.util.UUID;

/**
 * Public API of the Account module, consumable by other bounded contexts.
 * Synchronous queries are allowed across module boundaries per ADR-0008.
 */
public interface AccountApi {

    /**
     * Returns true if the account exists and has ACTIVE status.
     * Use for source-side (debit) validation — BLOCKED/PENDING_KYC accounts cannot send.
     */
    boolean existsAndActive(UUID accountId);

    /**
     * Returns true if the account exists regardless of status.
     * Use for destination-side (credit) validation — even BLOCKED accounts can receive funds.
     */
    boolean exists(UUID accountId);

    /**
     * Returns account information if found.
     */
    Optional<AccountInfo> findById(UUID accountId);
}
