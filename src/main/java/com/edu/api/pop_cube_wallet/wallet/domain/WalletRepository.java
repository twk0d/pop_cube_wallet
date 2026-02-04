package com.edu.api.pop_cube_wallet.wallet.domain;

import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.jmolecules.ddd.annotation.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Output port for Wallet persistence.
 */
@Repository
@SecondaryPort
public interface WalletRepository {

    Wallet save(Wallet wallet);

    Optional<Wallet> findByAccountId(UUID accountId);

    boolean existsByAccountId(UUID accountId);
}
