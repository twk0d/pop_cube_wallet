package com.edu.api.pop_cube_wallet.audit.web;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * REST response body for a single audit entry.
 */
@Schema(description = "A single immutable audit trail entry")
public record AuditEntryResponse(

        @Schema(description = "Audit entry UUID", example = "e5f6a7b8-c9d0-1234-ef01-23456789abcd")
        UUID id,

        @Schema(description = "Account UUID this entry belongs to", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID accountId,

        @Schema(description = "Event type: P2P_TRANSFER_SENT or P2P_TRANSFER_RECEIVED", example = "P2P_TRANSFER_SENT")
        String eventType,

        @Schema(description = "Human-readable description of the audited action",
                example = "Transfer of 250.00 to account b2c3d4e5-f6a7-8901-bcde-f12345678901 (tx=c3d4e5f6-a7b8-9012-cdef-123456789012)")
        String description,

        @Schema(description = "UTC timestamp of the original event", example = "2026-02-20T14:30:45.123")
        LocalDateTime occurredAt
) {}
