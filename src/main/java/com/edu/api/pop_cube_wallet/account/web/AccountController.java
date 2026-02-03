package com.edu.api.pop_cube_wallet.account.web;

import com.edu.api.pop_cube_wallet.account.AccountInfo;
import com.edu.api.pop_cube_wallet.account.application.CreateAccountCommand;
import com.edu.api.pop_cube_wallet.account.application.CreateAccountUseCase;
import com.edu.api.pop_cube_wallet.account.application.GetAccountQuery;
import com.edu.api.pop_cube_wallet.account.application.GetAccountUseCase;
import com.edu.api.pop_cube_wallet.shared.web.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Primary adapter: REST controller for the Account module.
 */
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@PrimaryAdapter
@Tag(name = "Accounts", description = "Account creation and lookup")
class AccountController {

    private final CreateAccountUseCase createAccountUseCase;
    private final GetAccountUseCase getAccountUseCase;

    @Operation(
            summary = "Create a new account",
            description = "Registers a new customer account with the given name, CPF, and email. "
                    + "A wallet with zero balance is automatically created via the AccountCreatedEvent flow."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AccountResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed or duplicate CPF/email",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        CreateAccountCommand command = new CreateAccountCommand(
                request.name(),
                request.cpf(),
                request.email()
        );
        AccountInfo info = createAccountUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(info));
    }

    @Operation(
            summary = "Get account by ID",
            description = "Retrieves account information for the given account ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AccountResponse.class))),
            @ApiResponse(responseCode = "404", description = "Account not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccount(
            @Parameter(description = "Account UUID", required = true,
                    example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
            @PathVariable UUID accountId) {
        AccountInfo info = getAccountUseCase.execute(new GetAccountQuery(accountId))
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + accountId));
        return ResponseEntity.ok(toResponse(info));
    }

    private AccountResponse toResponse(AccountInfo info) {
        return new AccountResponse(
                info.id(),
                info.fullName(),
                info.cpf(),
                info.email(),
                info.active()
        );
    }
}
