package com.example.scaffold.repository;

import com.example.scaffold.domain.inventory.Repair;

import java.util.List;

public interface RepairRepository extends IRepository<Repair, Long> {
    List<Repair> findAllOrderByIdDesc();
    List<Repair> findByWarehouseCodeOrderByIdDesc(String code);
    List<Repair> findByUserIdOrderByIdDesc(Long userId);
}

