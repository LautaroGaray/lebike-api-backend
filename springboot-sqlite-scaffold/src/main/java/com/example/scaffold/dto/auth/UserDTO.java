package com.example.scaffold.dto.auth;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class UserDTO {
    public String nickName;
    public String email;
    public String password;
    public Long roleId;
    public String roleName;
    public Role role;
    public LocalDateTime creationDate;
    public LocalDateTime editDate;
}
