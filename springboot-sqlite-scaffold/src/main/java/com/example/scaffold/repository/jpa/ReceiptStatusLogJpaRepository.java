package com.example.scaffold.repository.jpa;

import com.example.scaffold.domain.documents.ReceiptStatusLog;
import com.example.scaffold.repository.ReceiptStatusLogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public class ReceiptStatusLogJpaRepository implements ReceiptStatusLogRepository {

    @Value("${repository.query.receiptStatusLog.findAll}")
    private String findAllQuery;

    @Value("${repository.query.receiptStatusLog.findByReceiptIdOrderByChangedAtDesc}")
    private String findByReceiptIdOrderByChangedAtDescQuery;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public ReceiptStatusLog save(ReceiptStatusLog entity) {
        if (entity.getId() == null) {
            entityManager.persist(entity);
            return entity;
        }
        return entityManager.merge(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReceiptStatusLog> findById(Long id) {
        return Optional.ofNullable(entityManager.find(ReceiptStatusLog.class, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReceiptStatusLog> findAll() {
        return entityManager
                .createQuery(findAllQuery, ReceiptStatusLog.class)
                .getResultList();
    }

    @Override
    public void deleteById(Long id) {
        ReceiptStatusLog entity = entityManager.find(ReceiptStatusLog.class, id);
        if (entity != null) {
            entityManager.remove(entity);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReceiptStatusLog> findByReceiptIdOrderByChangedAtDesc(Long receiptId) {
        return entityManager
                .createQuery(findByReceiptIdOrderByChangedAtDescQuery, ReceiptStatusLog.class)
                .setParameter("receiptId", receiptId)
                .getResultList();
    }
}

