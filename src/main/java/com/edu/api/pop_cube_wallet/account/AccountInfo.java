package com.edu.api.pop_cube_wallet.account;

import java.util.UUID;

/**
 * Read-only DTO exposed by the Account module API to other modules.
 */
public record AccountInfo(
        UUID id,
        String fullName,
        String cpf,
        String email,
        boolean active
) {}
