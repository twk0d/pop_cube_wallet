/**
 * Transaction bounded context — the "Engine".
 * Orchestrates P2P transfers with idempotency, coordinates debit/credit via the Wallet API,
 * and publishes {@code TransactionCompletedEvent} for downstream projections and audit.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Transaction",
        allowedDependencies = {"account", "wallet", "shared", "shared :: web"}
)
package com.edu.api.pop_cube_wallet.transaction;
