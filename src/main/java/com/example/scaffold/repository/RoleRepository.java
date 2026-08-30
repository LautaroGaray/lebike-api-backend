package com.example.scaffold.repository;

import com.example.scaffold.domain.auths.Role;

import java.util.Optional;

public interface RoleRepository extends IRepository<Role, Long> {
    Optional<Role> findByName(String name);
}

