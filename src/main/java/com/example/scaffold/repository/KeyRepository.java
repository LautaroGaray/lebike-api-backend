package com.example.scaffold.repository;

import com.example.scaffold.domain.Audits.Keys;

import java.util.Optional;

public interface KeyRepository extends IRepository<Keys, Long> {
    Optional<Keys> findByTargetDestiny(String targetDestiny);

    Optional<Keys> findByTargetDestinyForUpdate(String targetDestiny);
}

