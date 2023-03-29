package org.auwerk.otus.arch.billingservice.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.auwerk.otus.arch.billingservice.domain.OperationType;

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
public class OperationDto {
    private UUID id;
    private OperationType type;
    private BigDecimal amount;
    private String comment;
    private LocalDateTime createdAt;
}
