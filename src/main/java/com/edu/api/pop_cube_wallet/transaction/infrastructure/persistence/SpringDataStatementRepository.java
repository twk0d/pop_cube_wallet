package com.edu.api.pop_cube_wallet.transaction.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataStatementRepository extends JpaRepository<StatementEntryJpaEntity, UUID> {

    List<StatementEntryJpaEntity> findByAccountIdOrderByCreatedAtDesc(UUID accountId);

    boolean existsByTransactionIdAndAccountId(UUID transactionId, UUID accountId);
}
