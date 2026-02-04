package com.edu.api.pop_cube_wallet.wallet.web;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Current wallet balance for an account")
public record BalanceResponse(

        @Schema(description = "Account UUID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID accountId,

        @Schema(description = "Current available balance in BRL", example = "1500.00")
        BigDecimal balance
) {}
