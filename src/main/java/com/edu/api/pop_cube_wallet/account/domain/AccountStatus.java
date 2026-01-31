package com.edu.api.pop_cube_wallet.account.domain;

/**
 * Account lifecycle states.
 * Phase 1 MVP: all accounts start as ACTIVE.
 * Phase 2 adds BLOCKED and PENDING_KYC state management.
 */
public enum AccountStatus {
    ACTIVE,
    BLOCKED,
    PENDING_KYC
}
