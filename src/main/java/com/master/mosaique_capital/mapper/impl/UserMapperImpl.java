// com/master/mosaique_capital/mapper/impl/UserMapperImpl.java
package com.master.mosaique_capital.mapper.impl;

import com.master.mosaique_capital.dto.auth.SignupRequest;
import com.master.mosaique_capital.entity.User;
import com.master.mosaique_capital.mapper.UserMapper;
import org.springframework.stereotype.Component;

import java.util.HashSet;

@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public User toEntity(SignupRequest dto) {
        if (dto == null) {
            return null;
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword()); // Sera encodé par le service
        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        user.setRoles(new HashSet<>());
        user.setActive(true);

        return user;
    }
}