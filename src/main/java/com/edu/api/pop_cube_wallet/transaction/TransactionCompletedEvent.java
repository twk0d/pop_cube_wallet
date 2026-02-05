package com.edu.api.pop_cube_wallet.transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain event published after a successful P2P transfer commit.
 * Placed in the module's root package so it is visible to other modules (ADR-0008).
 * Consumed by: Notification module, Statement projection handler.
 */
public record TransactionCompletedEvent(
        UUID transactionId,
        UUID sourceAccountId,
        UUID destinationAccountId,
        BigDecimal amount,
        String description,
        LocalDateTime completedAt
) {}
