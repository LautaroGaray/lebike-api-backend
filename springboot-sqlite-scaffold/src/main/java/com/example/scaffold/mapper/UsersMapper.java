package com.example.scaffold.mapper;

import com.example.scaffold.domain.Users;
import com.example.scaffold.dto.auth.UserDTO;
import com.example.scaffold.security.PasswordHasher;
import org.springframework.stereotype.Component;

@Component
public class UsersMapper {
    private final PasswordHasher passwordHasher;

    public UsersMapper(PasswordHasher passwordHasher) {
        this.passwordHasher = passwordHasher;
    }

    public Users toEntity(UserDTO dto) {
        if (dto == null) {
            return null;
        }

        String hashedPassword = passwordHasher.hash(dto.getPassword());
        return new Users(dto.getNickName(), hashedPassword, dto.getEmail(), null);
    }

    public UserDTO toDto(Users entity) {
        if (entity == null) {
            return null;
        }

        UserDTO dto = new UserDTO();
        dto.setNickName(entity.getUsername());
        dto.setEmail(entity.getEmail());
        dto.setPassword(null);
        dto.setRoleId(entity.getRole() != null ? entity.getRole().getId() : null);
        dto.setRoleName(entity.getRole() != null ? entity.getRole().getName() : null);
        if (entity.getRole() != null) {
            try {
                dto.setRole(com.example.scaffold.dto.auth.Role.valueOf(entity.getRole().getName()));
            } catch (IllegalArgumentException ignored) {
                dto.setRole(null);
            }
        }
        dto.setCreationDate(entity.getCreationDate());
        dto.setEditDate(entity.getEditDate());
        return dto;
    }

    public void updateEntityFromDto(UserDTO dto, Users entity) {
        if (dto == null || entity == null) {
            return;
        }

        if (dto.getNickName() != null) {
            entity.setUsername(dto.getNickName());
        }
        if (dto.getEmail() != null) {
            entity.setEmail(dto.getEmail());
        }
        if (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) {
            entity.setPasswordHash(passwordHasher.hash(dto.getPassword()));
        }
    }
}


