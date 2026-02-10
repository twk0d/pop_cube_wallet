package com.edu.api.pop_cube_wallet.audit.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity mapped to audit_schema.audit_entries.
 * Separated from the domain AuditEntry per Hexagonal Architecture (ADR-0004).
 */
@Entity
@Table(name = "audit_entries", schema = "audit_schema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuditEntryJpaEntity {

    @Id
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(length = 1000)
    private String description;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;
}
