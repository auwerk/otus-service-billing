package org.auwerk.otus.arch.billingservice.mapper;

import java.util.List;

import org.auwerk.otus.arch.billingservice.api.dto.OperationDto;
import org.auwerk.otus.arch.billingservice.domain.Operation;
import org.mapstruct.Mapper;

@Mapper(componentModel = "cdi")
public interface OperationMapper {

    OperationDto toDto(Operation operation);

    List<OperationDto> toDtos(List<Operation> operations);
}
