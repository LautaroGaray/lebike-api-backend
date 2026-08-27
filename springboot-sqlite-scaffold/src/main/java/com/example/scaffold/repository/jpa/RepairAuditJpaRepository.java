package com.example.scaffold.repository.jpa;

import com.example.scaffold.domain.Audits.RepairAudit;
import com.example.scaffold.repository.RepairAuditRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public class RepairAuditJpaRepository implements RepairAuditRepository {

    @Value("${repository.query.repairAudit.findAll}")
    private String findAllQuery;

    @Value("${repository.query.repairAudit.findByRepairId}")
    private String findByRepairIdQuery;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public RepairAudit save(RepairAudit entity) {
        if (entity.getId() == null) {
            entityManager.persist(entity);
            return entity;
        }
        return entityManager.merge(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RepairAudit> findById(Long id) {
        return Optional.ofNullable(entityManager.find(RepairAudit.class, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RepairAudit> findAll() {
        return entityManager.createQuery(findAllQuery, RepairAudit.class).getResultList();
    }

    @Override
    public void deleteById(Long id) {
        RepairAudit entity = entityManager.find(RepairAudit.class, id);
        if (entity != null) {
            entityManager.remove(entity);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<RepairAudit> findByRepairIdOrderByCreationDateDesc(Long repairId) {
        return entityManager.createQuery(findByRepairIdQuery, RepairAudit.class)
                .setParameter("repairId", repairId)
                .getResultList();
    }
}

