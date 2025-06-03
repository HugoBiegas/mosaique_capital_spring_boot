// com/master/mosaique_capital/mapper/BankConnectionMapper.java
package com.master.mosaique_capital.mapper;

import com.master.mosaique_capital.dto.banking.BankConnectionDto;
import com.master.mosaique_capital.entity.BankConnection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {BankAccountMapper.class})
public interface BankConnectionMapper {

    @Mapping(target = "accounts", source = "accounts")
    BankConnectionDto toDto(BankConnection bankConnection);

    List<BankConnectionDto> toDtoList(List<BankConnection> bankConnections);
}