package com.edu.api.pop_cube_wallet.transaction.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * REST request body for P2P transfer.
 */
@Schema(description = "Request body for executing a peer-to-peer transfer")
public record TransferRequest(

        @Schema(description = "Unique idempotency key — reuse the same key to safely retry a failed request",
                example = "txn-20260220-001")
        @NotBlank(message = "Deduplication key is required")
        String deduplicationKey,

        @Schema(description = "Recipient account UUID",
                example = "b2c3d4e5-f6a7-8901-bcde-f12345678901")
        @NotNull(message = "Destination account ID is required")
        UUID destinationAccountId,

        @Schema(description = "Transfer amount in BRL (minimum 0.01)",
                example = "250.00")
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
        BigDecimal amount,

        @Schema(description = "Optional transfer description",
                example = "Pagamento de aluguel")
        String description
) {}
