package com.edu.api.pop_cube_wallet.audit.infrastructure.persistence;

import com.edu.api.pop_cube_wallet.audit.domain.AuditEntry;
import com.edu.api.pop_cube_wallet.audit.domain.AuditEntryRepository;
import lombok.RequiredArgsConstructor;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Secondary adapter: bridges the domain AuditEntryRepository port to Spring Data JPA.
 */
@Component
@RequiredArgsConstructor
@SecondaryAdapter
class AuditPersistenceAdapter implements AuditEntryRepository {

    private final SpringDataAuditEntryRepository jpa;

    @Override
    public AuditEntry save(AuditEntry entry) {
        AuditEntryJpaEntity entity = toJpa(entry);
        AuditEntryJpaEntity saved = jpa.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<AuditEntry> findByAccountIdAndOccurredAtBetweenOrderByOccurredAtDesc(
            UUID accountId, LocalDateTime from, LocalDateTime to) {
        return jpa.findByAccountIdAndOccurredAtBetweenOrderByOccurredAtDesc(accountId, from, to)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsByAccountIdAndEventTypeAndOccurredAt(
            UUID accountId, String eventType, LocalDateTime occurredAt) {
        return jpa.existsByAccountIdAndEventTypeAndOccurredAt(accountId, eventType, occurredAt);
    }

    // ---- Mapping ----

    private AuditEntryJpaEntity toJpa(AuditEntry entry) {
        AuditEntryJpaEntity e = new AuditEntryJpaEntity();
        e.setId(entry.getId());
        e.setAccountId(entry.getAccountId());
        e.setEventType(entry.getEventType());
        e.setDescription(entry.getDescription());
        e.setOccurredAt(entry.getOccurredAt());
        return e;
    }

    private AuditEntry toDomain(AuditEntryJpaEntity e) {
        return AuditEntry.reconstitute(
                e.getId(),
                e.getAccountId(),
                e.getEventType(),
                e.getDescription(),
                e.getOccurredAt()
        );
    }
}
