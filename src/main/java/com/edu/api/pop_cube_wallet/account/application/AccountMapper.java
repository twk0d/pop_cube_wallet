package com.edu.api.pop_cube_wallet.account.application;

import com.edu.api.pop_cube_wallet.account.AccountInfo;
import com.edu.api.pop_cube_wallet.account.domain.Account;

/**
 * Package-private mapper — single source of truth for Account → AccountInfo conversion.
 */
final class AccountMapper {

    private AccountMapper() {}

    static AccountInfo toInfo(Account account) {
        return new AccountInfo(
                account.getId(),
                account.getFullName(),
                account.getCpf().digits(),
                account.getEmail().value(),
                account.isActive()
        );
    }
}
