package com.edu.api.pop_cube_wallet.transaction.web;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "A single entry in the account statement (CQRS read model)")
public record StatementEntryResponse(

        @Schema(description = "Statement entry UUID", example = "d4e5f6a7-b8c9-0123-def0-123456789abc")
        UUID id,

        @Schema(description = "Original transaction UUID", example = "c3d4e5f6-a7b8-9012-cdef-123456789012")
        UUID transactionId,

        @Schema(description = "Entry type: SENT or RECEIVED", example = "SENT")
        String type,

        @Schema(description = "Transfer amount in BRL", example = "250.00")
        BigDecimal amount,

        @Schema(description = "The other party's account UUID", example = "b2c3d4e5-f6a7-8901-bcde-f12345678901")
        UUID counterpartyAccountId,

        @Schema(description = "Transfer description (may be null)", example = "Pagamento de aluguel")
        String description,

        @Schema(description = "UTC timestamp of the entry", example = "2026-02-20T14:30:45.123")
        LocalDateTime createdAt
) {}
