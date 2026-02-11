/**
 * Notification bounded context — the "Alert".
 * Listens for domain events (e.g., {@code TransactionCompletedEvent}) and dispatches
 * user-facing notifications. Currently a mock logger; pluggable for email/SMS/push.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Notification",
        allowedDependencies = {"transaction", "shared", "shared :: web"}
)
package com.edu.api.pop_cube_wallet.notification;
