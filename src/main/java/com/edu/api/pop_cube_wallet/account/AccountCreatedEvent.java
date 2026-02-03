package com.edu.api.pop_cube_wallet.account;

import java.util.UUID;

/**
 * Domain event published after a new account is successfully created.
 * Consumed by the Wallet module to auto-create a wallet for the new account.
 */
public record AccountCreatedEvent(
        UUID accountId,
        String fullName
) {}
