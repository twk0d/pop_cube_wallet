package com.edu.api.pop_cube_wallet.account.application;

import com.edu.api.pop_cube_wallet.account.AccountCreatedEvent;
import com.edu.api.pop_cube_wallet.account.AccountInfo;
import com.edu.api.pop_cube_wallet.account.domain.Account;
import com.edu.api.pop_cube_wallet.account.domain.AccountRepository;
import com.edu.api.pop_cube_wallet.account.domain.Cpf;
import com.edu.api.pop_cube_wallet.account.domain.Email;
import com.edu.api.pop_cube_wallet.account.domain.AccountStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Command handler for account write operations.
 */
@Service
class AccountCommandHandler implements CreateAccountUseCase {

    private static final Logger log = LoggerFactory.getLogger(AccountCommandHandler.class);

    private final AccountRepository accountRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Counter accountCreatedCounter;

    AccountCommandHandler(AccountRepository accountRepository,
                          ApplicationEventPublisher eventPublisher,
                          MeterRegistry meterRegistry) {
        this.accountRepository = accountRepository;
        this.eventPublisher = eventPublisher;
        this.accountCreatedCounter = Counter.builder("wallet.account.created.count")
                .description("Number of accounts created")
                .register(meterRegistry);

        // Register one gauge per account status for real-time state distribution
        for (AccountStatus status : AccountStatus.values()) {
            Gauge.builder("wallet.account.state.gauge", accountRepository,
                            repo -> repo.countByStatus(status))
                    .tag("state", status.name())
                    .description("Current number of accounts in this state")
                    .register(meterRegistry);
        }
    }

    @Override
    @Transactional
    public AccountInfo execute(CreateAccountCommand command) {
        Cpf cpf = new Cpf(command.cpf());
        Email email = new Email(command.email());

        if (accountRepository.existsByCpf(cpf.digits())) {
            throw new IllegalArgumentException("An account with this CPF already exists");
        }
        if (accountRepository.existsByEmail(email.value())) {
            throw new IllegalArgumentException("An account with this email already exists");
        }

        Account account = Account.create(command.fullName(), cpf, email);
        Account saved = accountRepository.save(account);

        log.info("Account created: id={}, name={}", saved.getId(), saved.getFullName());

        accountCreatedCounter.increment();

        eventPublisher.publishEvent(new AccountCreatedEvent(saved.getId(), saved.getFullName()));

        return AccountMapper.toInfo(saved);
    }
}
