package com.edu.api.pop_cube_wallet.audit.domain;

import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.jmolecules.ddd.annotation.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Output port for AuditEntry persistence.
 * Implemented by the infrastructure layer adapter.
 */
@Repository
@SecondaryPort
public interface AuditEntryRepository {

    AuditEntry save(AuditEntry entry);

    List<AuditEntry> findByAccountIdAndOccurredAtBetweenOrderByOccurredAtDesc(
            UUID accountId, LocalDateTime from, LocalDateTime to);

    boolean existsByAccountIdAndEventTypeAndOccurredAt(
            UUID accountId, String eventType, LocalDateTime occurredAt);
}
