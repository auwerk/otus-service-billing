package org.auwerk.otus.arch.billingservice.exception;

public class InsufficentAccountBalanceException extends RuntimeException {
    
    public InsufficentAccountBalanceException() {
        super("insufficent account balance");
    }
}
