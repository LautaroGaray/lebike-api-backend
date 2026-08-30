package com.example.scaffold.repository;

import com.example.scaffold.domain.Audits.RepairAudit;

import java.util.List;

public interface RepairAuditRepository extends IRepository<RepairAudit, Long> {
    List<RepairAudit> findByRepairIdOrderByCreationDateDesc(Long repairId);
}

