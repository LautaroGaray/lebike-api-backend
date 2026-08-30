package com.example.scaffold.dto.auth;

public class TokenResponse {
    private final String token;
    private final String type;

    public TokenResponse(String token) {
        this.token = token;
        this.type = "Bearer";
    }

    public String getToken() {
        return token;
    }

    public String getType() {
        return type;
    }
}
