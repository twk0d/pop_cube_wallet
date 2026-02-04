package com.edu.api.pop_cube_wallet.wallet.web;

import com.edu.api.pop_cube_wallet.shared.web.ApiError;
import com.edu.api.pop_cube_wallet.wallet.application.GetBalanceQuery;
import com.edu.api.pop_cube_wallet.wallet.application.GetBalanceUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Primary adapter: REST controller for the Wallet module.
 * Auth simplified via User-ID header per MVP non‑functional requirements.
 */
@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
@PrimaryAdapter
@Tag(name = "Wallets", description = "Wallet balance queries")
class WalletController {

    private final GetBalanceUseCase getBalanceUseCase;

    @Operation(
            summary = "Get wallet balance",
            description = "Returns the current available balance for the authenticated user's wallet."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Balance retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = BalanceResponse.class))),
            @ApiResponse(responseCode = "404", description = "Wallet not found for the given account",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/balance")
    public ResponseEntity<BalanceResponse> getBalance(
            @Parameter(description = "Authenticated user's account UUID", required = true,
                    example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
            @RequestHeader("User-ID") UUID userId) {
        BigDecimal balance = getBalanceUseCase.execute(new GetBalanceQuery(userId));
        return ResponseEntity.ok(new BalanceResponse(userId, balance));
    }
}
