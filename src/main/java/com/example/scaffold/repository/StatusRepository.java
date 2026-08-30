package com.example.scaffold.repository;

import com.example.scaffold.domain.context.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StatusRepository extends JpaRepository<Status, Long> {
    Optional<Status> findByStatus(Integer status);

    boolean existsByStatus(Integer status);
}

