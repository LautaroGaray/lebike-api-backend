package com.example.scaffold.repository.jpa;

import com.example.scaffold.domain.Audits.ReceiptStatusHistory;
import com.example.scaffold.repository.ReceiptStatusHistoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public class ReceiptStatusHistoryJpaRepository implements ReceiptStatusHistoryRepository {

    @Value("${repository.query.receiptStatusHistory.findAll}")
    private String findAllQuery;

    @Value("${repository.query.receiptStatusHistory.findByReceiptId}")
    private String findByReceiptIdQuery;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public ReceiptStatusHistory save(ReceiptStatusHistory entity) {
        if (entity.getId() == null) {
            entityManager.persist(entity);
            return entity;
        }
        return entityManager.merge(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReceiptStatusHistory> findById(Long id) {
        return Optional.ofNullable(entityManager.find(ReceiptStatusHistory.class, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReceiptStatusHistory> findAll() {
        return entityManager.createQuery(findAllQuery, ReceiptStatusHistory.class).getResultList();
    }

    @Override
    public void deleteById(Long id) {
        ReceiptStatusHistory entity = entityManager.find(ReceiptStatusHistory.class, id);
        if (entity != null) {
            entityManager.remove(entity);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReceiptStatusHistory> findByReceiptIdOrderByCreationDateDesc(Long receiptId) {
        return entityManager.createQuery(findByReceiptIdQuery, ReceiptStatusHistory.class)
                .setParameter("receiptId", receiptId)
                .getResultList();
    }
}

