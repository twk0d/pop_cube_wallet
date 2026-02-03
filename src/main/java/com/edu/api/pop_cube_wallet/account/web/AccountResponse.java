package com.edu.api.pop_cube_wallet.account.web;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * REST response body for account information.
 */
@Schema(description = "Account details returned after creation or lookup")
public record AccountResponse(

        @Schema(description = "Generated account UUID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID id,

        @Schema(description = "Account holder's full name", example = "Maria Oliveira")
        String fullName,

        @Schema(description = "CPF digits (11 characters)", example = "12345678909")
        String cpf,

        @Schema(description = "Contact email address", example = "maria.oliveira@exemplo.com.br")
        String email,

        @Schema(description = "Whether the account is ACTIVE", example = "true")
        boolean active
) {}
