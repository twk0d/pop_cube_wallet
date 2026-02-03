package com.edu.api.pop_cube_wallet.account.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * REST request body for account creation.
 */
@Schema(description = "Request body for creating a new customer account")
public record CreateAccountRequest(

        @Schema(description = "Account holder's full name", example = "Maria Oliveira")
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
        String name,

        @Schema(description = "Brazilian CPF number (11 digits, optionally formatted)", example = "12345678909")
        @NotBlank(message = "CPF is required")
        @Size(min = 11, max = 14, message = "CPF must have 11 digits")
        String cpf,

        @Schema(description = "Contact email address", example = "maria.oliveira@exemplo.com.br")
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email
) {}
