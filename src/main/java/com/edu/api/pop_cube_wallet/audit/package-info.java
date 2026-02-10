/**
 * Audit bounded context — the "Record".
 * Provides an immutable audit trail of all financial movements.
 * Consumes domain events (e.g., {@code TransactionCompletedEvent}) and persists audit entries.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Audit",
        allowedDependencies = {"transaction", "shared", "shared :: web"}
)
package com.edu.api.pop_cube_wallet.audit;
