package com.example.scaffold.security;

import com.example.scaffold.dto.ResponseData;
import com.example.scaffold.service.auths.UserAuthorizationService;
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
@Component
public class BearerTokenInterceptor implements HandlerInterceptor {
    public static final String REQUEST_ROLE_ATTRIBUTE = BearerTokenInterceptor.class.getName() + ".ROLE";
    public static final String REQUEST_ROLE_ID_ATTRIBUTE = BearerTokenInterceptor.class.getName() + ".ROLE_ID";
    public static final String REQUEST_USER_ID_ATTRIBUTE = BearerTokenInterceptor.class.getName() + ".USER_ID";

    public static final String PARAM_MODULE_MAIN_ID = "main_id";
    public static final String PARAM_ACTION = "action";
    public static final String HEADER_MODULE_MAIN_ID = "X-Module-Main-Id";
    public static final String HEADER_ACTION = "X-Action";

    private final TokenService tokenService;
    private final UserAuthorizationService userAuthorizationService;
    private final ObjectMapper objectMapper;

    public BearerTokenInterceptor(TokenService tokenService,
                                  UserAuthorizationService userAuthorizationService,
                                  ObjectMapper objectMapper) {
        this.tokenService = tokenService;
        this.userAuthorizationService = userAuthorizationService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

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

            String action = request.getParameter(PARAM_ACTION);
            if (action == null || action.trim().isEmpty()) {
                action = request.getHeader(HEADER_ACTION);
            }
            if (action == null || action.trim().isEmpty()) {
                return reject(request, response, handler, HttpStatus.BAD_REQUEST,
                        "Missing required action (query param 'action' or header 'X-Action')");
            }

            String moduleMainId = request.getParameter(PARAM_MODULE_MAIN_ID);
            if (moduleMainId == null || moduleMainId.trim().isEmpty()) {
                moduleMainId = request.getHeader(HEADER_MODULE_MAIN_ID);
            }
            String normalizedAction = action.trim().toUpperCase();
            if (!UserAuthorizationService.ACTION_NONE.equals(normalizedAction)
                    && (moduleMainId == null || moduleMainId.trim().isEmpty())) {
                return reject(request, response, handler, HttpStatus.BAD_REQUEST,
                        "Missing required module id (query param 'main_id' or header 'X-Module-Main-Id')");
            }

            boolean granted = userAuthorizationService.canAccess(userId, roleId, moduleMainId, action);
            if (!granted) {
                return reject(request, response, handler, HttpStatus.FORBIDDEN,
                        "Insufficient permissions for action/module combination");
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
