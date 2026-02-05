package com.edu.api.pop_cube_wallet.transaction.application;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Result returned after a successful (or idempotent-duplicate) transfer.
 */
public record TransferResult(
        UUID transactionId,
        UUID sourceAccountId,
        UUID destinationAccountId,
        BigDecimal amount,
        String status,
        LocalDateTime createdAt
) {}
