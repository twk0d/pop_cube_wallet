package com.edu.api.pop_cube_wallet.account.application;

import com.edu.api.pop_cube_wallet.account.AccountInfo;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.Optional;

/**
 * Input port for querying account details.
 */
@PrimaryPort
public interface GetAccountUseCase {
    Optional<AccountInfo> execute(GetAccountQuery query);
}
