package org.auwerk.otus.arch.billingservice.exception;

import java.util.UUID;

import lombok.Getter;

public class OperationExecutedByDifferentUserException extends RuntimeException {

    @Getter
    private final UUID operationId;

    public OperationExecutedByDifferentUserException(UUID operationId) {
        super("operation executed by different user, id=" + operationId);
        this.operationId = operationId;
    }
}
