package com.edu.api.pop_cube_wallet.account.application;

import com.edu.api.pop_cube_wallet.account.AccountApi;
import com.edu.api.pop_cube_wallet.account.AccountInfo;
import com.edu.api.pop_cube_wallet.account.domain.Account;
import com.edu.api.pop_cube_wallet.account.domain.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Query handler for account read operations.
 * Also implements AccountApi so other modules can query account info.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class AccountQueryHandler implements GetAccountUseCase, AccountApi {

    private final AccountRepository accountRepository;

    @Override
    public Optional<AccountInfo> execute(GetAccountQuery query) {
        return accountRepository.findById(query.accountId()).map(AccountMapper::toInfo);
    }

    @Override
    public boolean existsAndActive(UUID accountId) {
        return accountRepository.findById(accountId)
                .map(Account::isActive)
                .orElse(false);
    }

    @Override
    public boolean exists(UUID accountId) {
        return accountRepository.existsById(accountId);
    }

    @Override
    public Optional<AccountInfo> findById(UUID accountId) {
        return accountRepository.findById(accountId).map(AccountMapper::toInfo);
    }
}
