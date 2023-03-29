package org.auwerk.otus.arch.billingservice.exception;

import java.util.UUID;

import lombok.Getter;

public class AccountNotFoundException extends RuntimeException {

    @Getter
    private UUID accountId;

    public AccountNotFoundException() {
        super("account not found");
    }

    public AccountNotFoundException(UUID accountId) {
        super("account not found, id=" + accountId);
        this.accountId = accountId;
    }
}
