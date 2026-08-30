package com.example.scaffold.repository;

import com.example.scaffold.domain.context.Module;

import java.util.Optional;

public interface ModuleRepository extends IRepository<Module, Long> {
    Optional<Module> findByMainId(String mainId);
    Optional<Module> findByName(String name);
}

