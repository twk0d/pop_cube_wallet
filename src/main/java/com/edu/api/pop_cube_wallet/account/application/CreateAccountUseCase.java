package com.edu.api.pop_cube_wallet.account.application;

import com.edu.api.pop_cube_wallet.account.AccountInfo;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

/**
 * Input port for account creation use case.
 */
@PrimaryPort
public interface CreateAccountUseCase {
    AccountInfo execute(CreateAccountCommand command);
}
