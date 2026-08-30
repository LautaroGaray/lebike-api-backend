package com.example.scaffold.service.auths;

import com.example.scaffold.security.BearerTokenInterceptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Centralizado service para validar permisos y autorizaciones de usuarios.
 *
 * Considera:
 * 1. Rol base del usuario (ADMIN, OWNER, etc.)
 * 2. Permisos a nivel de rol (en DB: role_permissions)
 * 3. Overrides a nivel de usuario (en DB: user_permissions)
 *
 * La precedencia es: User-level override > Role-level permission
 */
@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class UserAuthorizationService {

    public static final String ACTION_READ = "READ";
    public static final String ACTION_WRITE = "WRITE";
    public static final String ACTION_NONE = "NONE";

    private final ModulePermissionGateService modulePermissionGateService;

    public UserAuthorizationService(ModulePermissionGateService modulePermissionGateService) {
        this.modulePermissionGateService = modulePermissionGateService;
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public boolean canAccess(Long userId, Long roleId, String moduleMainId, String action) {
        if (userId == null || roleId == null || !StringUtils.hasText(action)) {
            return false;
        }

        String normalizedAction = action.trim().toUpperCase();
        if (ACTION_NONE.equals(normalizedAction)) {
            return true;
        }

        if (!StringUtils.hasText(moduleMainId)) {
            return false;
        }

        if (ACTION_READ.equals(normalizedAction)) {
            return modulePermissionGateService.canRead(userId, roleId, moduleMainId);
        }

        if (ACTION_WRITE.equals(normalizedAction)) {
            return modulePermissionGateService.canWrite(userId, roleId, moduleMainId);
        }

        return false;
    }


    public Long getRequesterUserId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        Object userAttribute = attributes.getRequest().getAttribute(BearerTokenInterceptor.REQUEST_USER_ID_ATTRIBUTE);
        return userAttribute instanceof Number ? ((Number) userAttribute).longValue() : null;
    }
}

