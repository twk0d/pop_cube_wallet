package com.edu.api.pop_cube_wallet.transaction.application;

import org.jmolecules.architecture.hexagonal.PrimaryPort;

/**
 * Input port for the P2P transfer use case.
 */
@PrimaryPort
public interface TransferUseCase {
    TransferResult execute(TransferCommand command);
}
