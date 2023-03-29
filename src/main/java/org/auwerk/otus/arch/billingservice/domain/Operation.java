package org.auwerk.otus.arch.billingservice.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Operation {
    private UUID id;
    private UUID accountId;
    private OperationType type;
    private BigDecimal amount;
    private String comment;
    private LocalDateTime createdAt;
}
