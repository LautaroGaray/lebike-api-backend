package com.example.scaffold.repository;

import com.example.scaffold.domain.inventory.BikeComponent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BikeComponentRepository extends JpaRepository<BikeComponent, Long> {
}
