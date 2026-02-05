package com.edu.api.pop_cube_wallet.transaction.application;

import org.jmolecules.architecture.cqrs.Command;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Command to execute a P2P transfer.
 */
@Command
public record TransferCommand(
        UUID sourceAccountId,
        UUID destinationAccountId,
        BigDecimal amount,
        String description,
        String idempotencyKey
) {}
