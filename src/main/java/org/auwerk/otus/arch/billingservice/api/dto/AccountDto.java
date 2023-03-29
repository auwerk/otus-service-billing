package org.auwerk.otus.arch.billingservice.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@RegisterForReflection
public class AccountDto {
    private UUID id;
    private BigDecimal balance;
    private LocalDateTime createdAt;
    private List<OperationDto> operations;
}
