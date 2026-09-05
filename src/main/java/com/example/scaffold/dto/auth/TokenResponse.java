package com.example.scaffold.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;

public class TokenResponse {
    private final String token;
    private final String type;
    private final String role;

    public TokenResponse(String token) {
        this(token, null);
    }

    public TokenResponse(String token, String role) {
        this.token = token;
        this.type = "Bearer";
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public String getType() {
        return type;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getRole() {
        return role;
    }
}
