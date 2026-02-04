package com.edu.api.pop_cube_wallet.wallet.application;

import org.jmolecules.architecture.cqrs.Command;

import java.math.BigDecimal;
import java.util.UUID;

@Command
public record DebitWalletCommand(
        UUID accountId,
        BigDecimal amount,
        UUID transactionId,
        String description
) {}
