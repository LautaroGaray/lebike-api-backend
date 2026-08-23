package com.example.scaffold.dto.auth;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserEditRequestDTO {
    private String currentEmail;
    private String currentNickName;
    private String newEmail;
    private String newNickName;
    private String password;
}

