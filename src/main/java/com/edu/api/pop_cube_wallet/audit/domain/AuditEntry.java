package com.edu.api.pop_cube_wallet.audit.domain;

import lombok.Getter;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Immutable audit entry representing a recorded financial event.
 * Once created, audit entries are never modified or deleted.
 */
@Entity
@Getter
public class AuditEntry {

    @Identity
    private UUID id;
    private UUID accountId;
    private String eventType;
    private String description;
    private LocalDateTime occurredAt;

    private AuditEntry() {}

    /**
     * Factory: creates a new audit entry with a UTC timestamp.
     */
    public static AuditEntry create(UUID accountId, String eventType, String description) {
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID must not be null");
        }
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("Event type must not be blank");
        }

        AuditEntry entry = new AuditEntry();
        entry.id = UUID.randomUUID();
        entry.accountId = accountId;
        entry.eventType = eventType;
        entry.description = description;
        entry.occurredAt = LocalDateTime.now(ZoneOffset.UTC);
        return entry;
    }

    /**
     * Reconstruct from persistence.
     */
    public static AuditEntry reconstitute(UUID id, UUID accountId, String eventType,
                                           String description, LocalDateTime occurredAt) {
        AuditEntry entry = new AuditEntry();
        entry.id = id;
        entry.accountId = accountId;
        entry.eventType = eventType;
        entry.description = description;
        entry.occurredAt = occurredAt;
        return entry;
    }
}
