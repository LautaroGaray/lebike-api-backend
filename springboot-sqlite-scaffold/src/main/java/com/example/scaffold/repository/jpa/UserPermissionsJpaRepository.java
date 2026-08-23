package com.example.scaffold.repository.jpa;

import com.example.scaffold.domain.UserPermissions;
import com.example.scaffold.repository.UserPermissionsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public class UserPermissionsJpaRepository implements UserPermissionsRepository {

    @Value("${repository.query.userPermissions.findAll}")
    private String findAllQuery;

    @Value("${repository.query.userPermissions.findByUserId}")
    private String findByUserIdQuery;

    @Value("${repository.query.userPermissions.findByUserModulePermission}")
    private String findByUserModulePermissionQuery;

    @Value("${repository.query.userPermissions.deleteByUserId}")
    private String deleteByUserIdQuery;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public UserPermissions save(UserPermissions entity) {
        if (entity.getId() == null) {
            entityManager.persist(entity);
            return entity;
        }
        return entityManager.merge(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserPermissions> findById(Long id) {
        return Optional.ofNullable(entityManager.find(UserPermissions.class, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserPermissions> findAll() {
        return entityManager.createQuery(findAllQuery, UserPermissions.class).getResultList();
    }

    @Override
    public void deleteById(Long id) {
        UserPermissions entity = entityManager.find(UserPermissions.class, id);
        if (entity != null) {
            entityManager.remove(entity);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserPermissions> findByUserIdAndModuleIdAndPermissionId(Long userId, Long moduleId, Long permissionId) {
        List<UserPermissions> rows = entityManager
                .createQuery(findByUserModulePermissionQuery, UserPermissions.class)
                .setParameter("userId", userId)
                .setParameter("moduleId", moduleId)
                .setParameter("permissionId", permissionId)
                .setMaxResults(1)
                .getResultList();
        return rows.stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserPermissions> findByUserId(Long userId) {
        return entityManager
                .createQuery(findByUserIdQuery, UserPermissions.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    @Override
    public void deleteByUserId(Long userId) {
        entityManager.createQuery(deleteByUserIdQuery)
                .setParameter("userId", userId)
                .executeUpdate();
    }
}

