package org.auwerk.otus.arch.billingservice.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Account {
    private UUID id;
    private String userName;
    private BigDecimal balance;
    private LocalDateTime createdAt;
    private List<Operation> operations;
}
