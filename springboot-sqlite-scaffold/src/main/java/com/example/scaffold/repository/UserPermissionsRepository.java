package com.example.scaffold.repository;

import com.example.scaffold.domain.UserPermissions;

import java.util.List;
import java.util.Optional;

public interface UserPermissionsRepository extends IRepository<UserPermissions, Long> {
    Optional<UserPermissions> findByUserIdAndModuleIdAndPermissionId(Long userId, Long moduleId, Long permissionId);

    List<UserPermissions> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}

