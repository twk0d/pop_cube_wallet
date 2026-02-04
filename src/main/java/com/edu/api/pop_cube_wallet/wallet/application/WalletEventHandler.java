package com.edu.api.pop_cube_wallet.wallet.application;

import com.edu.api.pop_cube_wallet.account.AccountCreatedEvent;
import com.edu.api.pop_cube_wallet.wallet.domain.Wallet;
import com.edu.api.pop_cube_wallet.wallet.domain.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Listens for AccountCreatedEvent and auto-creates a wallet for the new account.
 * Uses @ApplicationModuleListener for at-least-once delivery via Event Publication Registry.
 */
@Component
@RequiredArgsConstructor
class WalletEventHandler {

    private static final Logger log = LoggerFactory.getLogger(WalletEventHandler.class);

    private final WalletRepository walletRepository;

    @ApplicationModuleListener
    void onAccountCreated(AccountCreatedEvent event) {
        if (walletRepository.existsByAccountId(event.accountId())) {
            log.warn("Wallet already exists for account {}, skipping (idempotent)", event.accountId());
            return;
        }

        Wallet wallet = Wallet.create(event.accountId());
        walletRepository.save(wallet);

        log.info("Wallet created for account: id={}, name={}", event.accountId(), event.fullName());
    }
}
