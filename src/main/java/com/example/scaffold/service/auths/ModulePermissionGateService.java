package com.example.scaffold.service.auths;

import com.example.scaffold.domain.auths.Permissions;
import com.example.scaffold.domain.auths.RolePermissions;
import com.example.scaffold.domain.auths.UserPermissions;
import com.example.scaffold.domain.context.Module;
import com.example.scaffold.repository.ModuleRepository;
import com.example.scaffold.repository.PermissionRepository;
import com.example.scaffold.repository.RolePermissionsRepository;
import com.example.scaffold.repository.UserPermissionsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Optional;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class ModulePermissionGateService {

    private static final String PERMISSION_READ = "READ";
    private static final String PERMISSION_WRITE = "WRITE";

    private final ModuleRepository moduleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionsRepository rolePermissionsRepository;
    private final UserPermissionsRepository userPermissionsRepository;

    public ModulePermissionGateService(ModuleRepository moduleRepository,
                                       PermissionRepository permissionRepository,
                                       RolePermissionsRepository rolePermissionsRepository,
                                       UserPermissionsRepository userPermissionsRepository) {
        this.moduleRepository = moduleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionsRepository = rolePermissionsRepository;
        this.userPermissionsRepository = userPermissionsRepository;
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public boolean canRead(Long roleId, String moduleMainId) {
        return hasPermission(null, roleId, moduleMainId, PERMISSION_READ);
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public boolean canRead(Long userId, Long roleId, String moduleMainId) {
        return hasPermission(userId, roleId, moduleMainId, PERMISSION_READ);
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public boolean canWrite(Long roleId, String moduleMainId) {
        return hasPermission(null, roleId, moduleMainId, PERMISSION_WRITE);
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public boolean canWrite(Long userId, Long roleId, String moduleMainId) {
        return hasPermission(userId, roleId, moduleMainId, PERMISSION_WRITE);
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public boolean hasPermission(Long roleId, String moduleMainId, String permissionCode) {
        return hasPermission(null, roleId, moduleMainId, permissionCode);
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public boolean hasPermission(Long userId, Long roleId, String moduleMainId, String permissionCode) {
        if (roleId == null || !StringUtils.hasText(moduleMainId) || !StringUtils.hasText(permissionCode)) {
            return false;
        }

        Module module = moduleRepository.findByMainId(moduleMainId).orElse(null);
        if (module == null) {
            return false;
        }

        return hasPermissionForModule(userId, roleId, module, permissionCode);
    }

    private boolean hasPermissionForModule(Long userId, Long roleId, Module module, String permissionCode) {
        if (roleId == null || module == null || !StringUtils.hasText(permissionCode)) {
            return false;
        }

        String normalizedPermission = permissionCode.trim().toUpperCase(Locale.ROOT);
        Permissions permission = permissionRepository.findByCode(normalizedPermission).orElse(null);
        if (permission == null) {
            return false;
        }

        // 1) Base role permission from DB.
        boolean baseRolePermission = rolePermissionsRepository
                .findByRoleIdAndModuleIdAndPermissionId(roleId, module.getId(), permission.getId())
                .map(RolePermissions::isEnabled)
                .orElse(false);

        if (userId != null) {
            Optional<UserPermissions> userPermission = userPermissionsRepository
                    .findByUserIdAndModuleIdAndPermissionId(userId, module.getId(), permission.getId());
            if (userPermission.isPresent()) {
                // 2) User-level override wins over role-level defaults.
                return userPermission.get().isEnabled();
            }
        }

        return baseRolePermission;
    }

}


