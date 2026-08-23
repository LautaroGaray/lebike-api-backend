package com.example.scaffold.repository.jpa;

import com.example.scaffold.domain.RolePermissions;
import com.example.scaffold.repository.RolePermissionsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public class RolePermissionsJpaRepository implements RolePermissionsRepository {

    @Value("${repository.query.rolePermissions.findAll}")
    private String findAllQuery;

    @Value("${repository.query.rolePermissions.findByRoleId}")
    private String findByRoleIdQuery;

    @Value("${repository.query.rolePermissions.findByRoleModulePermission}")
    private String findByRoleModulePermissionQuery;

    @Value("${repository.query.rolePermissions.deleteByRoleId}")
    private String deleteByRoleIdQuery;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public RolePermissions save(RolePermissions entity) {
        if (entity.getId() == null) {
            entityManager.persist(entity);
            return entity;
        }
        return entityManager.merge(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RolePermissions> findById(Long id) {
        return Optional.ofNullable(entityManager.find(RolePermissions.class, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RolePermissions> findAll() {
        return entityManager.createQuery(findAllQuery, RolePermissions.class).getResultList();
    }

    @Override
    public void deleteById(Long id) {
        RolePermissions entity = entityManager.find(RolePermissions.class, id);
        if (entity != null) {
            entityManager.remove(entity);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RolePermissions> findByRoleIdAndModuleIdAndPermissionId(Long roleId, Long moduleId, Long permissionId) {
        List<RolePermissions> rows = entityManager
                .createQuery(findByRoleModulePermissionQuery, RolePermissions.class)
                .setParameter("roleId", roleId)
                .setParameter("moduleId", moduleId)
                .setParameter("permissionId", permissionId)
                .setMaxResults(1)
                .getResultList();
        return rows.stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RolePermissions> findByRoleId(Long roleId) {
        return entityManager
                .createQuery(findByRoleIdQuery, RolePermissions.class)
                .setParameter("roleId", roleId)
                .getResultList();
    }

    @Override
    public void deleteByRoleId(Long roleId) {
        entityManager.createQuery(deleteByRoleIdQuery)
                .setParameter("roleId", roleId)
                .executeUpdate();
    }
}

