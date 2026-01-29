-- V2: Account bounded context tables
CREATE TABLE account_schema.accounts (
    id              UUID            PRIMARY KEY,
    full_name       VARCHAR(255)    NOT NULL,
    cpf             VARCHAR(11)     NOT NULL UNIQUE,
    email           VARCHAR(255)    NOT NULL UNIQUE,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE INDEX idx_accounts_cpf ON account_schema.accounts (cpf);
CREATE INDEX idx_accounts_email ON account_schema.accounts (email);
CREATE INDEX idx_accounts_status ON account_schema.accounts (status);
