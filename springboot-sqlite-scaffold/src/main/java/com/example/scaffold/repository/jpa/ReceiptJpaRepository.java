package com.example.scaffold.repository.jpa;

import com.example.scaffold.domain.documents.Receipt;
import com.example.scaffold.repository.ReceiptRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public class ReceiptJpaRepository implements ReceiptRepository {

    @Value("${repository.query.receipts.findAll}")
    private String findAllQuery;

    @Value("${repository.query.receipts.findAllOrderByIdDesc}")
    private String findAllOrderByIdDescQuery;

    @Value("${repository.query.receipts.findByUserAndWarehouseOrderByIdDesc}")
    private String findByUserAndWarehouseOrderByIdDescQuery;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Receipt save(Receipt entity) {
        if (entity.getId() == null) {
            entityManager.persist(entity);
            return entity;
        }
        return entityManager.merge(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Receipt> findById(Long id) {
        return Optional.ofNullable(entityManager.find(Receipt.class, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Receipt> findAll() {
        return entityManager.createQuery(findAllQuery, Receipt.class).getResultList();
    }

    @Override
    public void deleteById(Long id) {
        Receipt entity = entityManager.find(Receipt.class, id);
        if (entity != null) {
            entityManager.remove(entity);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Receipt> findAllOrderByIdDesc() {
        return entityManager
                .createQuery(findAllOrderByIdDescQuery, Receipt.class)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Receipt> findByUserAndWarehouseOrderByIdDesc(Long userId, String warehouseCode) {
        return entityManager
                .createQuery(findByUserAndWarehouseOrderByIdDescQuery, Receipt.class)
                .setParameter("userId", userId)
                .setParameter("warehouseCode", warehouseCode)
                .getResultList();
    }
}


