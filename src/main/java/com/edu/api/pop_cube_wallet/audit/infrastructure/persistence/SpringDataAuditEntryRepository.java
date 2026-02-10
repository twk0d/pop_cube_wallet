package com.edu.api.pop_cube_wallet.audit.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for AuditEntryJpaEntity.
 */
public interface SpringDataAuditEntryRepository extends JpaRepository<AuditEntryJpaEntity, UUID> {

    List<AuditEntryJpaEntity> findByAccountIdAndOccurredAtBetweenOrderByOccurredAtDesc(
            UUID accountId, LocalDateTime from, LocalDateTime to);

    boolean existsByAccountIdAndEventTypeAndOccurredAt(
            UUID accountId, String eventType, LocalDateTime occurredAt);
}
