package com.edu.api.pop_cube_wallet.transaction.web;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Result of a completed P2P transfer")
public record TransferResponse(

        @Schema(description = "Generated transaction UUID", example = "c3d4e5f6-a7b8-9012-cdef-123456789012")
        UUID transactionId,

        @Schema(description = "Sender account UUID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID sourceAccountId,

        @Schema(description = "Recipient account UUID", example = "b2c3d4e5-f6a7-8901-bcde-f12345678901")
        UUID destinationAccountId,

        @Schema(description = "Transfer amount in BRL", example = "250.00")
        BigDecimal amount,

        @Schema(description = "Transaction status (always COMPLETED in MVP)", example = "COMPLETED")
        String status,

        @Schema(description = "UTC timestamp of the transaction", example = "2026-02-20T14:30:45.123")
        LocalDateTime createdAt
) {}
