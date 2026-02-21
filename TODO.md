# Docs

- [ ] Generate module dependency diagrams (Spring Modulith doc export)
- [ ] Add ADR for springdoc-openapi integration

# Code — Feature Work

- [ ] Account state transition API — endpoints to move accounts between ACTIVE / BLOCKED / PENDING_KYC
- [ ] Daily transfer limit policy — enforce configurable per-account daily debit cap before allowing new transfers
- [ ] Pockets / sub-accounts — partitioned balance within a wallet; transfers cannot spend pocket funds without authorization
- [ ] Compensating transactions — issue reversal/estorno when downstream steps fail (saga-style inside monolith)

# Code — Infrastructure & Resilience

- [ ] Retry / DLQ configuration for Spring Modulith Event Publication Registry (max retries, dead-letter handling)

# Code — Quality & Testing

- [ ] Load tests for concurrent opposite-direction P2P transfers (verify deadlock-free lock ordering)
- [ ] Unit tests for domain aggregates (Account, Wallet, Transaction value-object validation)
- [ ] Integration tests for each REST controller (MockMvc + H2)