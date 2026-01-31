package com.edu.api.pop_cube_wallet.account.infrastructure.persistence;

import com.edu.api.pop_cube_wallet.account.domain.Account;
import com.edu.api.pop_cube_wallet.account.domain.AccountRepository;
import com.edu.api.pop_cube_wallet.account.domain.AccountStatus;
import lombok.RequiredArgsConstructor;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Secondary adapter: bridges the domain AccountRepository port to Spring Data JPA.
 */
@Component
@RequiredArgsConstructor
@SecondaryAdapter
class AccountPersistenceAdapter implements AccountRepository {

    private final SpringDataAccountRepository jpa;

    @Override
    public Account save(Account account) {
        AccountJpaEntity entity = toJpa(account);
        AccountJpaEntity saved = jpa.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Account> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public boolean existsByCpf(String cpf) {
        return jpa.existsByCpf(cpf);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpa.existsByEmail(email);
    }

    @Override
    public boolean existsById(UUID id) {
        return jpa.existsById(id);
    }

    @Override
    public long countByStatus(AccountStatus status) {
        return jpa.countByStatus(status);
    }

    // ---- Mapping (manual to avoid MapStruct complexity with value objects) ----

    private AccountJpaEntity toJpa(Account account) {
        AccountJpaEntity entity = new AccountJpaEntity();
        entity.setId(account.getId());
        entity.setFullName(account.getFullName());
        entity.setCpf(account.getCpf().digits());
        entity.setEmail(account.getEmail().value());
        entity.setStatus(account.getStatus());
        entity.setCreatedAt(account.getCreatedAt());
        entity.setUpdatedAt(account.getUpdatedAt());
        return entity;
    }

    private Account toDomain(AccountJpaEntity entity) {
        return Account.reconstitute(
                entity.getId(),
                entity.getFullName(),
                entity.getCpf(),
                entity.getEmail(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
