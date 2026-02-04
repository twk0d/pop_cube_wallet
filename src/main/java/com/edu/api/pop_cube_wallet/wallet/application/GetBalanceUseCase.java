package com.edu.api.pop_cube_wallet.wallet.application;

import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.math.BigDecimal;

/**
 * Input port for querying wallet balance.
 */
@PrimaryPort
public interface GetBalanceUseCase {
    BigDecimal execute(GetBalanceQuery query);
}
