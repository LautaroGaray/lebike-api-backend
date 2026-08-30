package com.example.scaffold.controller;

import com.example.scaffold.dto.ResponseData;
import com.example.scaffold.dto.auth.TokenResponse;
import com.example.scaffold.dto.auth.UserDTO;
import com.example.scaffold.security.TokenService;
import com.example.scaffold.service.auths.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Objects;


@RestController
@RequestMapping("/auth")
public class AuthController {
    private final TokenService tokenService;

    private final UserService userService;

    public AuthController(TokenService tokenService, UserService userService) {
        this.tokenService = tokenService;
        this.userService = userService;
    }

    @PostMapping("login")
    public ResponseEntity<ResponseData> login(@RequestBody(required = true) UserDTO userDTO, HttpServletRequest request) {
        if(Objects.isNull(userDTO)){
            return ResponseEntity.badRequest().body(new ResponseData(null, false, "User data is null"));
        }

        if (!StringUtils.hasText(userDTO.getEmail()) || !StringUtils.hasText(userDTO.getPassword())) {
            return ResponseEntity.badRequest().body(new ResponseData(null, false, "Email or password is empty"));
        }

        UserDTO userFromDb = userService.validateCredentials(userDTO.getEmail(), userDTO.getPassword()).orElse(null);
        if (userFromDb == null) {
            return ResponseEntity.status(401).body(new ResponseData(null, false, "Invalid email or password"));
        }

        HttpSession session = request.getSession(true);
		Object currentToken = session.getAttribute(TokenService.SESSION_TOKEN);
		if (currentToken instanceof String) {
			tokenService.invalidateToken((String) currentToken);
		}
		session.removeAttribute(TokenService.SESSION_TOKEN);
        String token = tokenService.getOrCreateToken(session, userFromDb.getId(), userFromDb.getRoleId(), userFromDb.getRoleName());

        ResponseData responseData = new ResponseData(new TokenResponse(token), true, "Login successful");

        return ResponseEntity.ok(responseData);

    }

    @PostMapping("/token")
    public TokenResponse token(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        return new TokenResponse(tokenService.getOrCreateToken(session));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        // Blacklistear el JWT para que no pueda reutilizarse aunque no haya expirado
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            tokenService.invalidateToken(authorization.substring(7).trim());
        }
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.noContent().build();
    }
}
