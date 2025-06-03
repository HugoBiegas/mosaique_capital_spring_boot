// com/master/mosaique_capital/mapper/BankTransactionMapper.java
package com.master.mosaique_capital.mapper;

import com.master.mosaique_capital.dto.banking.BankTransactionDto;
import com.master.mosaique_capital.entity.BankTransaction;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BankTransactionMapper {

    BankTransactionDto toDto(BankTransaction bankTransaction);

    List<BankTransactionDto> toDtoList(List<BankTransaction> bankTransactions);
}
