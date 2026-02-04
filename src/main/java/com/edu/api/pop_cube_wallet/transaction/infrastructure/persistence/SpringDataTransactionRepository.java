package com.edu.api.pop_cube_wallet.transaction.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataTransactionRepository extends JpaRepository<TransactionJpaEntity, UUID> {

    Optional<TransactionJpaEntity> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);
}
