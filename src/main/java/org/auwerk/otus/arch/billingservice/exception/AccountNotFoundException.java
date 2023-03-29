package org.auwerk.otus.arch.billingservice.exception;

public class AccountNotFoundException extends RuntimeException {
    
    public AccountNotFoundException() {
        super("account not found");
    }
}
