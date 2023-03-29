package org.auwerk.otus.arch.billingservice.exception;

import java.util.UUID;

import lombok.Getter;

public class OperationNotFoundException extends RuntimeException {

    @Getter
    private final UUID operationId;

    public OperationNotFoundException(UUID operationId) {
        super("operation not found, id=" + operationId);
        this.operationId = operationId;
    }
}
