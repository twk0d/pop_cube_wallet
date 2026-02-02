package com.edu.api.pop_cube_wallet.account.application;

import org.jmolecules.architecture.cqrs.Command;

/**
 * Command to create a new account during onboarding.
 */
@Command
public record CreateAccountCommand(
        String fullName,
        String cpf,
        String email
) {}
