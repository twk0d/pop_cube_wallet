-- V5: Audit bounded context tables
CREATE SCHEMA IF NOT EXISTS audit_schema;

CREATE TABLE audit_schema.audit_entries (
    id              UUID            PRIMARY KEY,
    account_id      UUID            NOT NULL,
    event_type      VARCHAR(50)     NOT NULL,
    description     VARCHAR(1000),
    occurred_at     TIMESTAMP       NOT NULL
);

CREATE INDEX idx_audit_account_id ON audit_schema.audit_entries (account_id);
CREATE INDEX idx_audit_occurred_at ON audit_schema.audit_entries (occurred_at);
CREATE INDEX idx_audit_account_date ON audit_schema.audit_entries (account_id, occurred_at DESC);
CREATE INDEX idx_audit_idempotency ON audit_schema.audit_entries (account_id, event_type, occurred_at);
