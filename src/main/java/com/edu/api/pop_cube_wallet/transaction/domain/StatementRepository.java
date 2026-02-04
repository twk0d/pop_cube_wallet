package com.edu.api.pop_cube_wallet.transaction.domain;

import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.jmolecules.ddd.annotation.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Output port for StatementEntry persistence (CQRS read model).
 */
@Repository
@SecondaryPort
public interface StatementRepository {

    StatementEntry save(StatementEntry entry);

    List<StatementEntry> findByAccountIdOrderByCreatedAtDesc(UUID accountId);

    boolean existsByTransactionIdAndAccountId(UUID transactionId, UUID accountId);
}
