package com.edu.api.pop_cube_wallet.account.infrastructure.persistence;

import com.edu.api.pop_cube_wallet.account.domain.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Spring Data JPA repository for AccountJpaEntity.
 */
public interface SpringDataAccountRepository extends JpaRepository<AccountJpaEntity, UUID> {

    boolean existsByCpf(String cpf);

    boolean existsByEmail(String email);

    long countByStatus(AccountStatus status);
}
