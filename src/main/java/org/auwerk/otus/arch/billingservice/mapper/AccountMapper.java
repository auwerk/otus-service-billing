package org.auwerk.otus.arch.billingservice.mapper;

import org.auwerk.otus.arch.billingservice.api.dto.AccountDto;
import org.auwerk.otus.arch.billingservice.domain.Account;
import org.mapstruct.Mapper;

@Mapper(componentModel = "cdi", uses = OperationMapper.class)
public interface AccountMapper {
    
    AccountDto toDto(Account account);
}
