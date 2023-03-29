package org.auwerk.otus.arch.billingservice.exception;

public class AccountAlreadyExistsException extends RuntimeException {
    
    public AccountAlreadyExistsException() {
        super("account already exists");
    }
}
