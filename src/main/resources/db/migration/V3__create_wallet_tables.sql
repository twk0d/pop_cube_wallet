-- V3: Wallet bounded context tables
CREATE TABLE wallet_schema.wallets (
    id              UUID            PRIMARY KEY,
    account_id      UUID            NOT NULL UNIQUE,
    balance         NUMERIC(19,2)   NOT NULL DEFAULT 0.00,
    version         BIGINT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE INDEX idx_wallets_account_id ON wallet_schema.wallets (account_id);

CREATE TABLE wallet_schema.ledger_entries (
    id              UUID            PRIMARY KEY,
    wallet_id       UUID            NOT NULL REFERENCES wallet_schema.wallets(id),
    entry_type      VARCHAR(10)     NOT NULL,
    amount          NUMERIC(19,2)   NOT NULL,
    transaction_id  UUID            NOT NULL,
    description     VARCHAR(500),
    created_at      TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE INDEX idx_ledger_wallet_id ON wallet_schema.ledger_entries (wallet_id);
CREATE INDEX idx_ledger_transaction_id ON wallet_schema.ledger_entries (transaction_id);
CREATE INDEX idx_ledger_created_at ON wallet_schema.ledger_entries (created_at);
