package com.example.scaffold.repository;

import com.example.scaffold.domain.Permissions;

import java.util.Optional;

public interface PermissionRepository extends IRepository<Permissions, Long> {
    Optional<Permissions> findByCode(String code);
}

