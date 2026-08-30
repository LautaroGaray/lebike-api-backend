package com.example.scaffold.repository.jpa;

import com.example.scaffold.domain.auths.Permissions;
import com.example.scaffold.repository.PermissionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public class PermissionJpaRepository implements PermissionRepository {

    @Value("${repository.query.permissions.findAll}")
    private String findAllQuery;

    @Value("${repository.query.permissions.findByCode}")
    private String findByCodeQuery;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Permissions save(Permissions entity) {
        if (entity.getId() == null) {
            entityManager.persist(entity);
            return entity;
        }
        return entityManager.merge(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Permissions> findById(Long id) {
        return Optional.ofNullable(entityManager.find(Permissions.class, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Permissions> findAll() {
        return entityManager.createQuery(findAllQuery, Permissions.class).getResultList();
    }

    @Override
    public void deleteById(Long id) {
        Permissions entity = entityManager.find(Permissions.class, id);
        if (entity != null) {
            entityManager.remove(entity);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Permissions> findByCode(String code) {
        List<Permissions> permissions = entityManager
                .createQuery(findByCodeQuery, Permissions.class)
                .setParameter("code", code == null ? null : code.trim().toUpperCase())
                .setMaxResults(1)
                .getResultList();
        return permissions.stream().findFirst();
    }
}

