package com.example.scaffold.repository;

import com.example.scaffold.domain.auths.RolePermissions;

import java.util.List;
import java.util.Optional;

public interface RolePermissionsRepository extends IRepository<RolePermissions, Long> {
    Optional<RolePermissions> findByRoleIdAndModuleIdAndPermissionId(Long roleId, Long moduleId, Long permissionId);

    List<RolePermissions> findByRoleId(Long roleId);

    void deleteByRoleId(Long roleId);
}

