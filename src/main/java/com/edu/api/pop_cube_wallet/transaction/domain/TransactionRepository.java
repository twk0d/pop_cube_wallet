package com.edu.api.pop_cube_wallet.transaction.domain;

import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.jmolecules.ddd.annotation.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Output port for Transaction persistence.
 */
@Repository
@SecondaryPort
public interface TransactionRepository {

    Transaction save(Transaction transaction);

    Optional<Transaction> findById(UUID id);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);
}
