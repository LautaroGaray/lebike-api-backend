package com.example.scaffold.dto.auth;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class UserDTO {
    public Long id;
    public String nickName;
    public String email;
    public String password;
    public Long roleId;
    public String roleName;
    public Role role;
    public List<Long> warehouseIds;
    public List<String> warehouseCodes;
    public LocalDateTime creationDate;
    public LocalDateTime editDate;
}
