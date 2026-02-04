package com.edu.api.pop_cube_wallet.wallet.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataWalletRepository extends JpaRepository<WalletJpaEntity, UUID> {

    Optional<WalletJpaEntity> findByAccountId(UUID accountId);

    boolean existsByAccountId(UUID accountId);
}
