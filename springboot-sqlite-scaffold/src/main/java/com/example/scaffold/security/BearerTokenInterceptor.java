package com.example.scaffold.security;

import com.example.scaffold.domain.auths.Role;
import com.example.scaffold.dto.ResponseData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
public class BearerTokenInterceptor implements HandlerInterceptor {
    public static final String REQUEST_ROLE_ATTRIBUTE = BearerTokenInterceptor.class.getName() + ".ROLE";
    public static final String REQUEST_ROLE_ID_ATTRIBUTE = BearerTokenInterceptor.class.getName() + ".ROLE_ID";
    public static final String REQUEST_USER_ID_ATTRIBUTE = BearerTokenInterceptor.class.getName() + ".USER_ID";

    private static final Set<String> REGISTER_ENDPOINT_SUFFIXES = Set.of(
            "/users/register",
            "/auth/register",
            "/users/edit"
    );

    private static final Set<String> REGISTER_ENDPOINT_SUFFIXES_OWNER = Set.of(
            "/users/delete",
            "/users/editRole"
    );

    private final TokenService tokenService;
    private final ObjectMapper objectMapper;

    public BearerTokenInterceptor(TokenService tokenService, ObjectMapper objectMapper) {
        this.tokenService = tokenService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        try {
            String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                return reject(request, response, handler, HttpStatus.UNAUTHORIZED, "Missing bearer token");
            }

            String token = authorization.substring(7).trim();
            if (!tokenService.isValid(token)) {
                return reject(request, response, handler, HttpStatus.UNAUTHORIZED, "Invalid or expired bearer token");
            }

            String roleName = tokenService.getRoleName(token);
            Long roleId = tokenService.getRoleId(token);
            Long userId = tokenService.getUserId(token);
            request.setAttribute(REQUEST_ROLE_ATTRIBUTE, roleName);
            request.setAttribute(REQUEST_ROLE_ID_ATTRIBUTE, roleId);
            request.setAttribute(REQUEST_USER_ID_ATTRIBUTE, userId);

            String path = request.getRequestURI();
            boolean isRegisterEndpoint = REGISTER_ENDPOINT_SUFFIXES.stream().anyMatch(path::endsWith);
            boolean isRegisterEndpointOwner = REGISTER_ENDPOINT_SUFFIXES_OWNER.stream().anyMatch(path::endsWith);

            if (isRegisterEndpoint && !(Role.ADMIN.equals(roleName) || Role.OWNER.equals(roleName))) {
                return reject(request, response, handler, HttpStatus.FORBIDDEN, "Insufficient role permissions");
            }
            if (isRegisterEndpointOwner && !Role.OWNER.equals(roleName)) {
                return reject(request, response, handler, HttpStatus.FORBIDDEN, "Insufficient role permissions");
            }
            return true;
        } catch (Exception e) {
            return reject(request, response, handler, HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected authentication error");
        }
    }

    private boolean reject(HttpServletRequest request, HttpServletResponse response, Object handler,
                           HttpStatus status, String message) {
        if (response.isCommitted()) {
            return false;
        }

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        try {
            objectMapper.writeValue(response.getWriter(), new ResponseData(null, false, message));
            response.flushBuffer();
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }
}
