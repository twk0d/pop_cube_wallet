package com.edu.api.pop_cube_wallet.account.domain;

import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.jmolecules.ddd.annotation.Repository;

import java.util.Optional;
import java.util.UUID;


/**
 * Output port for Account persistence.
 * Implemented by the infrastructure layer adapter.
 */
@Repository
@SecondaryPort
public interface AccountRepository {

    Account save(Account account);

    Optional<Account> findById(UUID id);

    boolean existsByCpf(String cpf);

    boolean existsByEmail(String email);

    boolean existsById(UUID id);

    long countByStatus(AccountStatus status);
}
