package com.example.scaffold.repository.jpa;

import com.example.scaffold.domain.inventory.Repair;
import com.example.scaffold.repository.RepairRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public class RepairJpaRepository implements RepairRepository {

    @Value("${repository.query.repair.findAll}")
    private String findAllQuery;

    @Value("${repository.query.repair.findAllOrderByIdDesc}")
    private String findAllOrderByIdDescQuery;

    @Value("${repository.query.repair.findByWarehouseCode}")
    private String findByWarehouseCodeQuery;

    @Value("${repository.query.repair.findByUserId}")
    private String findByUserIdQuery;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Repair save(Repair entity) {
        if (entity.getId() == null) {
            entityManager.persist(entity);
            return entity;
        }
        return entityManager.merge(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Repair> findById(Long id) {
        return Optional.ofNullable(entityManager.find(Repair.class, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Repair> findAll() {
        return entityManager.createQuery(findAllQuery, Repair.class).getResultList();
    }

    @Override
    public void deleteById(Long id) {
        Repair entity = entityManager.find(Repair.class, id);
        if (entity != null) {
            entityManager.remove(entity);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Repair> findAllOrderByIdDesc() {
        return entityManager.createQuery(findAllOrderByIdDescQuery, Repair.class).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Repair> findByWarehouseCodeOrderByIdDesc(String code) {
        return entityManager.createQuery(findByWarehouseCodeQuery, Repair.class)
                .setParameter("code", code)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Repair> findByUserIdOrderByIdDesc(Long userId) {
        return entityManager.createQuery(findByUserIdQuery, Repair.class)
                .setParameter("userId", userId)
                .getResultList();
    }
}

