# 📡 API Reference

Complete REST API reference for the Digital Wallet API. All endpoints return JSON.

**Base URL:** `http://localhost:8080`

**Interactive docs:** [Swagger UI](http://localhost:8080/swagger-ui.html) | [OpenAPI JSON](http://localhost:8080/api-docs)

---

## Authentication

The API uses a simplified `User-ID` header for authentication. Endpoints that require it are marked in the table below.

| Header | Type | Description |
|--------|------|-------------|
| `User-ID` | `UUID` | The account ID of the authenticated user. Required for wallet, transfer, and statement endpoints. |

> **Note:** This is a simplified MVP authentication pattern. Production systems should use OAuth2/JWT. See [MVP doc](./docs/project-MVP.md) for details.

---

## Endpoints Overview

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `POST` | `/api/accounts` | Create a new account | None |
| `GET` | `/api/accounts/{accountId}` | Get account by ID | None |
| `GET` | `/api/wallets/balance` | Get wallet balance | `User-ID` |
| `POST` | `/api/transactions/transfer` | Execute P2P transfer | `User-ID` |
| `GET` | `/api/transactions/statement` | Get account statement | `User-ID` |
| `GET` | `/api/audit` | Query audit trail | None |

---

## Error Response Format

All error responses use a standardized `ApiError` format, produced by the `GlobalExceptionHandler`:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "CPF must have 11 digits",
  "timestamp": "2026-02-20T14:30:00.000"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `status` | `int` | HTTP status code |
| `error` | `String` | Error category (e.g., "Bad Request", "Not Found") |
| `message` | `String` | Human-readable error description |
| `timestamp` | `LocalDateTime` | UTC timestamp of the error |

### Exception Mapping

| Exception | HTTP Status | When |
|-----------|-------------|------|
| `MethodArgumentNotValidException` | `400` | Request body fails `@Valid` constraints (field-level details joined by `;`) |
| `IllegalArgumentException` | `400` | Domain validation failure (e.g., duplicate CPF, invalid amount) |
| `EntityNotFoundException` | `404` | Resource not found (account, wallet, transaction) |
| `IllegalStateException` | `409` | Business rule conflict (e.g., insufficient balance, inactive account) |
| `Exception` (catch-all) | `500` | Unexpected server error |

---

## Accounts

### `POST /api/accounts` — Create Account

Creates a new account with the given name, CPF, and email. A wallet with zero balance is automatically created via the `AccountCreatedEvent` → `WalletEventHandler` flow.

**Request Body:**

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `name` | `String` | Yes | Not blank, 2–255 characters |
| `cpf` | `String` | Yes | Not blank, 11–14 characters (11 digits, optionally formatted) |
| `email` | `String` | Yes | Not blank, valid email format |

> **CPF validation:** Beyond the `@Size` constraint, the domain `Cpf` value object validates that the string contains exactly 11 digits, rejects all-identical digits (e.g., `11111111111`), and verifies the two check digits using the official algorithm.

**Response:** `201 Created`

| Field | Type | Description |
|-------|------|-------------|
| `id` | `UUID` | Generated account ID |
| `fullName` | `String` | Account holder's full name |
| `cpf` | `String` | CPF digits (11 chars) |
| `email` | `String` | Email address |
| `active` | `boolean` | Whether the account is ACTIVE |

**Error Codes:**

| Status | Cause |
|--------|-------|
| `400` | Validation failed (blank name, invalid CPF format, invalid email) |
| `400` | Duplicate CPF or email (`IllegalArgumentException`) |

**curl Example:**

```bash
curl -X POST http://localhost:8080/api/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "name": "João Silva",
    "cpf": "12345678909",
    "email": "joao@example.com"
  }'
```

**Response:**

```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "fullName": "João Silva",
  "cpf": "12345678909",
  "email": "joao@example.com",
  "active": true
}
```

---

### `GET /api/accounts/{accountId}` — Get Account

Retrieves account information by ID.

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `accountId` | `UUID` | The account ID |

**Response:** `200 OK`

| Field | Type | Description |
|-------|------|-------------|
| `id` | `UUID` | Account ID |
| `fullName` | `String` | Account holder's full name |
| `cpf` | `String` | CPF digits |
| `email` | `String` | Email address |
| `active` | `boolean` | Whether the account is ACTIVE |

**Error Codes:**

| Status | Cause |
|--------|-------|
| `404` | Account not found |

**curl Example:**

```bash
curl -X GET http://localhost:8080/api/accounts/a1b2c3d4-e5f6-7890-abcd-ef1234567890
```

---

## Wallets

### `GET /api/wallets/balance` — Get Balance

Retrieves the current wallet balance for the authenticated user.

**Headers:**

| Header | Type | Required | Description |
|--------|------|----------|-------------|
| `User-ID` | `UUID` | Yes | The account ID to query |

**Response:** `200 OK`

| Field | Type | Description |
|-------|------|-------------|
| `accountId` | `UUID` | The account ID |
| `balance` | `BigDecimal` | Current available balance (e.g., `1500.00`) |

**Error Codes:**

| Status | Cause |
|--------|-------|
| `404` | Wallet not found for the given account |

**curl Example:**

```bash
curl -X GET http://localhost:8080/api/wallets/balance \
  -H "User-ID: a1b2c3d4-e5f6-7890-abcd-ef1234567890"
```

**Response:**

```json
{
  "accountId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "balance": 1500.00
}
```

---

## Transactions

### `POST /api/transactions/transfer` — P2P Transfer

Executes a peer-to-peer transfer from the authenticated user's account to a destination account. The entire flow (validation → debit → credit → persist → event) runs in a single database transaction for atomicity.

**Headers:**

| Header | Type | Required | Description |
|--------|------|----------|-------------|
| `User-ID` | `UUID` | Yes | The source (sender) account ID |

**Request Body:**

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `deduplicationKey` | `String` | Yes | Not blank — unique idempotency key |
| `destinationAccountId` | `UUID` | Yes | Not null — recipient account ID |
| `amount` | `BigDecimal` | Yes | Not null, minimum `0.01` |
| `description` | `String` | No | Optional transfer description |

**Response:** `201 Created`

| Field | Type | Description |
|-------|------|-------------|
| `transactionId` | `UUID` | Generated transaction ID |
| `sourceAccountId` | `UUID` | Sender account ID |
| `destinationAccountId` | `UUID` | Recipient account ID |
| `amount` | `BigDecimal` | Transfer amount |
| `status` | `String` | Always `"COMPLETED"` in MVP |
| `createdAt` | `LocalDateTime` | UTC timestamp |

**Error Codes:**

| Status | Cause |
|--------|-------|
| `400` | Validation failed (blank key, null amount, amount < 0.01) |
| `400` | Same source and destination account |
| `404` | Source account not found |
| `404` | Destination account not found |
| `409` | Source account is not ACTIVE (BLOCKED or PENDING_KYC) |
| `409` | Insufficient balance |

#### Idempotency

Every transfer request must include a `deduplicationKey`. If a transfer with the same key has already been processed, the API returns the original transaction result with `201 Created` — it does **not** execute the transfer again. This prevents double-posting in case of network retries or client failures.

**Best practices:**
- Generate a UUID v4 client-side for each unique transfer intent.
- Retry the same request with the same key on timeout/failure.
- Do **not** reuse keys across different transfer intents.

**curl Example:**

```bash
curl -X POST http://localhost:8080/api/transactions/transfer \
  -H "Content-Type: application/json" \
  -H "User-ID: a1b2c3d4-e5f6-7890-abcd-ef1234567890" \
  -d '{
    "deduplicationKey": "txn-20260220-001",
    "destinationAccountId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
    "amount": 250.00,
    "description": "Rent payment"
  }'
```

**Response:**

```json
{
  "transactionId": "c3d4e5f6-a7b8-9012-cdef-123456789012",
  "sourceAccountId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "destinationAccountId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
  "amount": 250.00,
  "status": "COMPLETED",
  "createdAt": "2026-02-20T14:30:45.123"
}
```

---

### `GET /api/transactions/statement` — Account Statement

Returns the account's transaction history as a list of statement entries. This is the **CQRS read model** — a denormalized projection fed asynchronously by `TransactionCompletedEvent`.

Each P2P transfer produces two statement entries: `SENT` for the source account and `RECEIVED` for the destination account.

**Headers:**

| Header | Type | Required | Description |
|--------|------|----------|-------------|
| `User-ID` | `UUID` | Yes | The account ID to query |

**Response:** `200 OK` — `List<StatementEntryResponse>`

| Field | Type | Description |
|-------|------|-------------|
| `id` | `UUID` | Statement entry ID |
| `transactionId` | `UUID` | Original transaction ID |
| `type` | `String` | `"SENT"` or `"RECEIVED"` |
| `amount` | `BigDecimal` | Transfer amount |
| `counterpartyAccountId` | `UUID` | The other party's account ID |
| `description` | `String` | Transfer description (may be null) |
| `createdAt` | `LocalDateTime` | UTC timestamp |

> **Eventual consistency:** Statement entries are created asynchronously after the transfer commits. There may be a brief delay (typically < 1 second) between a transfer completing and the statement reflecting it.

**curl Example:**

```bash
curl -X GET http://localhost:8080/api/transactions/statement \
  -H "User-ID: a1b2c3d4-e5f6-7890-abcd-ef1234567890"
```

**Response:**

```json
[
  {
    "id": "d4e5f6a7-b8c9-0123-def0-123456789abc",
    "transactionId": "c3d4e5f6-a7b8-9012-cdef-123456789012",
    "type": "SENT",
    "amount": 250.00,
    "counterpartyAccountId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
    "description": "Rent payment",
    "createdAt": "2026-02-20T14:30:45.123"
  }
]
```

---

## Audit

### `GET /api/audit` — Query Audit Trail

Returns the immutable audit trail for a specific account within a time range. Audit entries are created asynchronously from `TransactionCompletedEvent` — each P2P transfer produces two entries: `P2P_TRANSFER_SENT` (source) and `P2P_TRANSFER_RECEIVED` (destination).

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `accountId` | `UUID` | Yes | The account ID to query |
| `from` | `LocalDateTime` | Yes | Start of time range (ISO 8601, e.g., `2026-01-01T00:00:00`) |
| `to` | `LocalDateTime` | Yes | End of time range (ISO 8601, e.g., `2026-12-31T23:59:59`) |

**Response:** `200 OK` — `List<AuditEntryResponse>`

| Field | Type | Description |
|-------|------|-------------|
| `id` | `UUID` | Audit entry ID |
| `accountId` | `UUID` | Account ID |
| `eventType` | `String` | `"P2P_TRANSFER_SENT"` or `"P2P_TRANSFER_RECEIVED"` |
| `description` | `String` | Human-readable description of the movement |
| `occurredAt` | `LocalDateTime` | UTC timestamp of the original event |

**curl Example:**

```bash
curl -X GET "http://localhost:8080/api/audit?accountId=a1b2c3d4-e5f6-7890-abcd-ef1234567890&from=2026-01-01T00:00:00&to=2026-12-31T23:59:59"
```

**Response:**

```json
[
  {
    "id": "e5f6a7b8-c9d0-1234-ef01-23456789abcd",
    "accountId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "eventType": "P2P_TRANSFER_SENT",
    "description": "Transfer of 250.00 to account b2c3d4e5-f6a7-8901-bcde-f12345678901 (tx=c3d4e5f6-a7b8-9012-cdef-123456789012)",
    "occurredAt": "2026-02-20T14:30:45.123"
  }
]
```

---

## Actuator Endpoints

The following Spring Boot Actuator endpoints are enabled:

| Path | Description |
|------|-------------|
| `/actuator/health` | Application health (includes DB status) |
| `/actuator/info` | Application info |
| `/actuator/metrics` | Micrometer metrics index |
| `/actuator/prometheus` | Prometheus-format metrics scrape endpoint |

---

## Related Documents

| Document | Description |
|----------|-------------|
| [README.md](./README.md) | Project overview and quick start |
| [ARCHITECTURE.md](./ARCHITECTURE.md) | System architecture, CQRS, event flows |
| [EVENTS.md](./EVENTS.md) | Domain event catalog with payloads and idempotency |
| [DEVELOPMENT.md](./DEVELOPMENT.md) | Developer onboarding and local setup |
