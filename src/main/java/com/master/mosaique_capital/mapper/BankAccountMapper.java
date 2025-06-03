// com/master/mosaique_capital/mapper/BankAccountMapper.java
package com.master.mosaique_capital.mapper;

import com.master.mosaique_capital.dto.banking.BankAccountDto;
import com.master.mosaique_capital.entity.BankAccount;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {BankTransactionMapper.class})
public interface BankAccountMapper {


    @Mapping(target = "recentTransactions", ignore = true)
    BankAccountDto toDto(BankAccount bankAccount);

    List<BankAccountDto> toDtoList(List<BankAccount> bankAccounts);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "connection", ignore = true)
    @Mapping(target = "transactions", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    BankAccount toEntity(BankAccountDto dto);
}