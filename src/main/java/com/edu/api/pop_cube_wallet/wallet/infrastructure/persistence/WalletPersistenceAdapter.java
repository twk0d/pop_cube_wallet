package com.edu.api.pop_cube_wallet.wallet.infrastructure.persistence;

import com.edu.api.pop_cube_wallet.wallet.domain.LedgerEntry;
import com.edu.api.pop_cube_wallet.wallet.domain.LedgerEntryRepository;
import com.edu.api.pop_cube_wallet.wallet.domain.Wallet;
import com.edu.api.pop_cube_wallet.wallet.domain.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Secondary adapter: bridges domain Wallet/Ledger ports to Spring Data JPA.
 */
@Component
@RequiredArgsConstructor
@SecondaryAdapter
class WalletPersistenceAdapter implements WalletRepository, LedgerEntryRepository {

    private final SpringDataWalletRepository walletJpa;
    private final SpringDataLedgerEntryRepository ledgerJpa;

    // ---- WalletRepository ----

    @Override
    public Wallet save(Wallet wallet) {
        WalletJpaEntity entity = toJpa(wallet);
        WalletJpaEntity saved = walletJpa.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Wallet> findByAccountId(UUID accountId) {
        return walletJpa.findByAccountId(accountId).map(this::toDomain);
    }

    @Override
    public boolean existsByAccountId(UUID accountId) {
        return walletJpa.existsByAccountId(accountId);
    }

    // ---- LedgerEntryRepository ----

    @Override
    public LedgerEntry save(LedgerEntry entry) {
        LedgerEntryJpaEntity entity = toJpa(entry);
        LedgerEntryJpaEntity saved = ledgerJpa.save(entity);
        return toDomain(saved);
    }

    // ---- Wallet mapping ----

    private WalletJpaEntity toJpa(Wallet wallet) {
        WalletJpaEntity e = new WalletJpaEntity();
        e.setId(wallet.getId());
        e.setAccountId(wallet.getAccountId());
        e.setBalance(wallet.getBalance());
        e.setVersion(wallet.getVersion());
        e.setCreatedAt(wallet.getCreatedAt());
        e.setUpdatedAt(wallet.getUpdatedAt());
        return e;
    }

    private Wallet toDomain(WalletJpaEntity e) {
        return Wallet.reconstitute(
                e.getId(), e.getAccountId(), e.getBalance(),
                e.getVersion(), e.getCreatedAt(), e.getUpdatedAt()
        );
    }

    // ---- LedgerEntry mapping ----

    private LedgerEntryJpaEntity toJpa(LedgerEntry entry) {
        LedgerEntryJpaEntity e = new LedgerEntryJpaEntity();
        e.setId(entry.getId());
        e.setWalletId(entry.getWalletId());
        e.setEntryType(entry.getEntryType());
        e.setAmount(entry.getAmount());
        e.setTransactionId(entry.getTransactionId());
        e.setDescription(entry.getDescription());
        e.setCreatedAt(entry.getCreatedAt());
        return e;
    }

    private LedgerEntry toDomain(LedgerEntryJpaEntity e) {
        return LedgerEntry.reconstitute(
                e.getId(), e.getWalletId(), e.getEntryType(),
                e.getAmount(), e.getTransactionId(),
                e.getDescription(), e.getCreatedAt()
        );
    }
}
