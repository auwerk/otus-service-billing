package org.auwerk.otus.arch.billingservice.exception;

import java.util.UUID;

import lombok.Getter;

public class OperationAlreadyCanceledException extends RuntimeException {

    @Getter
    private final UUID operationId;

    public OperationAlreadyCanceledException(UUID operationId) {
        super("operation already canceled, id=" + operationId);
        this.operationId = operationId;
    }
}
