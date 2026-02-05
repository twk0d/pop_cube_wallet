package com.edu.api.pop_cube_wallet.transaction.web;

import com.edu.api.pop_cube_wallet.shared.web.ApiError;
import com.edu.api.pop_cube_wallet.transaction.application.GetStatementQuery;
import com.edu.api.pop_cube_wallet.transaction.application.GetStatementUseCase;
import com.edu.api.pop_cube_wallet.transaction.application.TransferCommand;
import com.edu.api.pop_cube_wallet.transaction.application.TransferResult;
import com.edu.api.pop_cube_wallet.transaction.application.TransferUseCase;
import com.edu.api.pop_cube_wallet.transaction.domain.StatementEntry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Primary adapter: REST controller for the Transaction module.
 * Uses User-ID header for simplified authentication (MVP scope).
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@PrimaryAdapter
@Tag(name = "Transactions", description = "P2P transfers and account statements")
class TransactionController {

    private final TransferUseCase transferUseCase;
    private final GetStatementUseCase getStatementUseCase;

    @Operation(
            summary = "Execute a P2P transfer",
            description = "Transfers funds from the authenticated user's account to a destination account. "
                    + "The entire flow (validation, debit, credit, persist, event) runs in a single database "
                    + "transaction. Duplicate requests with the same deduplicationKey return the original result."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transfer completed (or idempotent duplicate returned)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TransferResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed (blank key, null amount, amount < 0.01, same source/destination)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Source or destination account not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Source account not active or insufficient balance",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transfer(
            @Parameter(description = "Authenticated sender's account UUID", required = true,
                    example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
            @RequestHeader("User-ID") UUID userId,
            @Valid @RequestBody TransferRequest request) {

        TransferCommand command = new TransferCommand(
                userId,
                request.destinationAccountId(),
                request.amount(),
                request.description(),
                request.deduplicationKey()
        );

        TransferResult result = transferUseCase.execute(command);

        TransferResponse response = new TransferResponse(
                result.transactionId(),
                result.sourceAccountId(),
                result.destinationAccountId(),
                result.amount(),
                result.status(),
                result.createdAt()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Get account statement",
            description = "Returns the CQRS read-model statement for the authenticated user. "
                    + "Each P2P transfer produces two entries: SENT (source) and RECEIVED (destination). "
                    + "Entries are projected asynchronously — brief delay after transfer is expected."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statement entries retrieved",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = StatementEntryResponse.class)))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/statement")
    public ResponseEntity<List<StatementEntryResponse>> getStatement(
            @Parameter(description = "Authenticated user's account UUID", required = true,
                    example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
            @RequestHeader("User-ID") UUID userId) {

        List<StatementEntry> entries = getStatementUseCase.execute(new GetStatementQuery(userId));

        List<StatementEntryResponse> response = entries.stream()
                .map(e -> new StatementEntryResponse(
                        e.getId(),
                        e.getTransactionId(),
                        e.getEntryType(),
                        e.getAmount(),
                        e.getCounterpartyAccountId(),
                        e.getDescription(),
                        e.getCreatedAt()
                ))
                .toList();

        return ResponseEntity.ok(response);
    }
}
