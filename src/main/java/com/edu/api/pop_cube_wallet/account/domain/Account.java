package com.edu.api.pop_cube_wallet.account.domain;

import lombok.Getter;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Account aggregate root — the "Identity" bounded context.
 * Pure domain object with no framework dependencies (except jMolecules markers).
 */
@AggregateRoot
@Getter
public class Account {

    @Identity
    private UUID id;
    private String fullName;
    private Cpf cpf;
    private Email email;
    private AccountStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Account() {
        // Used by factory method and mapper reconstruction
    }

    /**
     * Factory method for creating a new account during onboarding.
     */
    public static Account create(String fullName, Cpf cpf, Email email) {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name must not be blank");
        }

        Account account = new Account();
        account.id = UUID.randomUUID();
        account.fullName = fullName.trim();
        account.cpf = cpf;
        account.email = email;
        account.status = AccountStatus.ACTIVE;
        account.createdAt = LocalDateTime.now(ZoneOffset.UTC);
        account.updatedAt = LocalDateTime.now(ZoneOffset.UTC);
        return account;
    }

    /**
     * Reconstruct an Account from persistence. Used by the infrastructure mapper.
     */
    public static Account reconstitute(UUID id, String fullName, String cpf, String email,
                                        AccountStatus status, LocalDateTime createdAt,
                                        LocalDateTime updatedAt) {
        Account account = new Account();
        account.id = id;
        account.fullName = fullName;
        account.cpf = new Cpf(cpf);
        account.email = new Email(email);
        account.status = status;
        account.createdAt = createdAt;
        account.updatedAt = updatedAt;
        return account;
    }

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }
}
