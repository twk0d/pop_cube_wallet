/**
 * Wallet bounded context — the "Vault".
 * Manages balances via double-entry ledger, supports credit and debit operations
 * with optimistic locking for concurrency safety.
 * Reacts to {@code AccountCreatedEvent} to auto-provision wallets.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Wallet",
        allowedDependencies = {"account", "shared", "shared :: web"}
)
package com.edu.api.pop_cube_wallet.wallet;
