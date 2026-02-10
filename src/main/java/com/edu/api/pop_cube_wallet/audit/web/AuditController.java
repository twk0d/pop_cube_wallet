package com.edu.api.pop_cube_wallet.audit.web;

import com.edu.api.pop_cube_wallet.audit.application.GetAuditQuery;
import com.edu.api.pop_cube_wallet.audit.application.GetAuditUseCase;
import com.edu.api.pop_cube_wallet.audit.domain.AuditEntry;
import com.edu.api.pop_cube_wallet.shared.web.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Primary adapter: REST controller for the Audit module.
 * Provides a query endpoint to list audit history by account and date range.
 */
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@PrimaryAdapter
@Tag(name = "Audit", description = "Immutable audit trail queries")
class AuditController {

    private final GetAuditUseCase getAuditUseCase;

    @Operation(
            summary = "Query audit trail",
            description = "Returns the immutable audit trail for a specific account within a time range. "
                    + "Each P2P transfer produces two entries: P2P_TRANSFER_SENT (source) and "
                    + "P2P_TRANSFER_RECEIVED (destination). Entries are created asynchronously."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Audit entries retrieved",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = AuditEntryResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Missing or invalid query parameters",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public ResponseEntity<List<AuditEntryResponse>> getAuditTrail(
            @Parameter(description = "Account UUID to query", required = true,
                    example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
            @RequestParam UUID accountId,
            @Parameter(description = "Start of time range (ISO 8601)", required = true,
                    example = "2026-01-01T00:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "End of time range (ISO 8601)", required = true,
                    example = "2026-12-31T23:59:59")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        List<AuditEntry> entries = getAuditUseCase.execute(new GetAuditQuery(accountId, from, to));

        List<AuditEntryResponse> response = entries.stream()
                .map(e -> new AuditEntryResponse(
                        e.getId(),
                        e.getAccountId(),
                        e.getEventType(),
                        e.getDescription(),
                        e.getOccurredAt()
                ))
                .toList();

        return ResponseEntity.ok(response);
    }
}
