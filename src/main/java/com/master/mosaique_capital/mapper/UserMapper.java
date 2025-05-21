// com/master/mosaique_capital/mapper/UserMapper.java
package com.master.mosaique_capital.mapper;

import com.master.mosaique_capital.dto.auth.SignupRequest;
import com.master.mosaique_capital.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "mfaEnabled", constant = "false")
    @Mapping(target = "mfaSecret", ignore = true)
    @Mapping(target = "active", constant = "true")
    User toEntity(SignupRequest dto);
}