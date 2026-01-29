-- V4: Transaction bounded context tables (write + CQRS read model)
CREATE TABLE transaction_schema.transactions (
    id                      UUID            PRIMARY KEY,
    source_account_id       UUID            NOT NULL,
    destination_account_id  UUID            NOT NULL,
    amount                  NUMERIC(19,2)   NOT NULL,
    description             VARCHAR(500),
    status                  VARCHAR(20)     NOT NULL,
    idempotency_key         VARCHAR(255)    NOT NULL UNIQUE,
    created_at              TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE INDEX idx_transactions_source ON transaction_schema.transactions (source_account_id);
CREATE INDEX idx_transactions_destination ON transaction_schema.transactions (destination_account_id);
CREATE INDEX idx_transactions_idempotency ON transaction_schema.transactions (idempotency_key);
CREATE INDEX idx_transactions_created_at ON transaction_schema.transactions (created_at);

-- CQRS Read Model: denormalized statement projection (ADR-0006)
CREATE TABLE transaction_schema.statement_entries (
    id                      UUID            PRIMARY KEY,
    account_id              UUID            NOT NULL,
    transaction_id          UUID            NOT NULL,
    entry_type              VARCHAR(20)     NOT NULL,
    amount                  NUMERIC(19,2)   NOT NULL,
    counterparty_account_id UUID            NOT NULL,
    description             VARCHAR(500),
    created_at              TIMESTAMP       NOT NULL
);

CREATE INDEX idx_statement_account_id ON transaction_schema.statement_entries (account_id);
CREATE INDEX idx_statement_created_at ON transaction_schema.statement_entries (created_at);
CREATE INDEX idx_statement_account_date ON transaction_schema.statement_entries (account_id, created_at DESC);
