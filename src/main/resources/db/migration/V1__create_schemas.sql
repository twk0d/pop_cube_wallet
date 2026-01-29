-- V1: Create isolated schemas for each bounded context (ADR-0007)
CREATE SCHEMA IF NOT EXISTS account_schema;
CREATE SCHEMA IF NOT EXISTS wallet_schema;
CREATE SCHEMA IF NOT EXISTS transaction_schema;
