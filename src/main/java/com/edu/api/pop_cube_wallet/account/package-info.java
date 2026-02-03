/**
 * Account bounded context — the "Identity".
 * Manages customer onboarding, KYC state, and account lifecycle (ACTIVE / BLOCKED / PENDING_KYC).
 * Publishes {@code AccountCreatedEvent} consumed by the Wallet module to provision a new wallet.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Account",
        allowedDependencies = {"shared", "shared :: web"}
)
package com.edu.api.pop_cube_wallet.account;
