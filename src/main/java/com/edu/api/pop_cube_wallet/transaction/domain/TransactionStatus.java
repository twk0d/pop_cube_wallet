package com.edu.api.pop_cube_wallet.transaction.domain;

/**
 * Status of a financial transaction.
 * Only COMPLETED is used in MVP — failures roll back the entire @Transactional boundary,
 * so no Transaction entity is persisted. Recording failures would require a separate
 * transaction (REQUIRES_NEW) or an event-based approach, deferred to a future phase.
 */
public enum TransactionStatus {
    COMPLETED
}
