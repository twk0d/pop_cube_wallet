package com.edu.api.pop_cube_wallet.transaction.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity for the CQRS statement read model.
 */
@Entity
@Table(name = "statement_entries", schema = "transaction_schema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StatementEntryJpaEntity {

    @Id
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "entry_type", nullable = false, length = 20)
    private String entryType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "counterparty_account_id", nullable = false)
    private UUID counterpartyAccountId;

    @Column(length = 500)
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
