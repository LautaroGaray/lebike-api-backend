package com.example.scaffold.repository;

import com.example.scaffold.domain.Users;

import java.util.Optional;

public interface UserRepository extends IRepository<Users, Long> {
    Optional<Users> findByEmail(String email);

    Optional<Users> findByUsername(String username);


}

