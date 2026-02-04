package com.edu.api.pop_cube_wallet.transaction.infrastructure.persistence;

import com.edu.api.pop_cube_wallet.transaction.domain.StatementEntry;
import com.edu.api.pop_cube_wallet.transaction.domain.StatementRepository;
import com.edu.api.pop_cube_wallet.transaction.domain.Transaction;
import com.edu.api.pop_cube_wallet.transaction.domain.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Secondary adapter: bridges domain Transaction/Statement ports to Spring Data JPA.
 */
@Component
@RequiredArgsConstructor
@SecondaryAdapter
class TransactionPersistenceAdapter implements TransactionRepository, StatementRepository {

    private final SpringDataTransactionRepository transactionJpa;
    private final SpringDataStatementRepository statementJpa;

    // ---- TransactionRepository ----

    @Override
    public Transaction save(Transaction transaction) {
        TransactionJpaEntity entity = toJpa(transaction);
        TransactionJpaEntity saved = transactionJpa.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Transaction> findById(UUID id) {
        return transactionJpa.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Transaction> findByIdempotencyKey(String idempotencyKey) {
        return transactionJpa.findByIdempotencyKey(idempotencyKey).map(this::toDomain);
    }

    @Override
    public boolean existsByIdempotencyKey(String idempotencyKey) {
        return transactionJpa.existsByIdempotencyKey(idempotencyKey);
    }

    // ---- StatementRepository ----

    @Override
    public StatementEntry save(StatementEntry entry) {
        StatementEntryJpaEntity entity = toJpa(entry);
        StatementEntryJpaEntity saved = statementJpa.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<StatementEntry> findByAccountIdOrderByCreatedAtDesc(UUID accountId) {
        return statementJpa.findByAccountIdOrderByCreatedAtDesc(accountId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsByTransactionIdAndAccountId(UUID transactionId, UUID accountId) {
        return statementJpa.existsByTransactionIdAndAccountId(transactionId, accountId);
    }

    // ---- Transaction mapping ----

    private TransactionJpaEntity toJpa(Transaction tx) {
        TransactionJpaEntity e = new TransactionJpaEntity();
        e.setId(tx.getId());
        e.setSourceAccountId(tx.getSourceAccountId());
        e.setDestinationAccountId(tx.getDestinationAccountId());
        e.setAmount(tx.getAmount());
        e.setDescription(tx.getDescription());
        e.setStatus(tx.getStatus());
        e.setIdempotencyKey(tx.getIdempotencyKey());
        e.setCreatedAt(tx.getCreatedAt());
        return e;
    }

    private Transaction toDomain(TransactionJpaEntity e) {
        return Transaction.reconstitute(
                e.getId(), e.getSourceAccountId(), e.getDestinationAccountId(),
                e.getAmount(), e.getDescription(), e.getStatus(),
                e.getIdempotencyKey(), e.getCreatedAt()
        );
    }

    // ---- StatementEntry mapping ----

    private StatementEntryJpaEntity toJpa(StatementEntry entry) {
        StatementEntryJpaEntity e = new StatementEntryJpaEntity();
        e.setId(entry.getId());
        e.setAccountId(entry.getAccountId());
        e.setTransactionId(entry.getTransactionId());
        e.setEntryType(entry.getEntryType());
        e.setAmount(entry.getAmount());
        e.setCounterpartyAccountId(entry.getCounterpartyAccountId());
        e.setDescription(entry.getDescription());
        e.setCreatedAt(entry.getCreatedAt());
        return e;
    }

    private StatementEntry toDomain(StatementEntryJpaEntity e) {
        return StatementEntry.reconstitute(
                e.getId(), e.getAccountId(), e.getTransactionId(),
                e.getEntryType(), e.getAmount(), e.getCounterpartyAccountId(),
                e.getDescription(), e.getCreatedAt()
        );
    }
}
