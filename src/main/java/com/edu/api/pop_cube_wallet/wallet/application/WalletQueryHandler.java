package com.edu.api.pop_cube_wallet.wallet.application;

import com.edu.api.pop_cube_wallet.wallet.domain.Wallet;
import com.edu.api.pop_cube_wallet.wallet.domain.WalletRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Query handler for wallet read operations used by the web layer.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class WalletQueryHandler implements GetBalanceUseCase {

    private final WalletRepository walletRepository;

    @Override
    public BigDecimal execute(GetBalanceQuery query) {
        return walletRepository.findByAccountId(query.accountId())
                .map(Wallet::getBalance)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Wallet not found for account: " + query.accountId()));
    }
}
