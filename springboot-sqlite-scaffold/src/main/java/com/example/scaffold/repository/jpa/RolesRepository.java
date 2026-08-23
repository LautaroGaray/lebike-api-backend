package com.example.scaffold.repository.jpa;

import com.example.scaffold.domain.Role;
import com.example.scaffold.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public class RolesRepository implements RoleRepository {

    @Value("${repository.query.roles.findAll}")
    private String findAllQuery;

    @Value("${repository.query.roles.findByName}")
    private String findByNameQuery;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Role save(Role entity) {
        if (entity.getId() == null) {
            entityManager.persist(entity);
            return entity;
        }
        return entityManager.merge(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Role> findById(Long id) {
        return Optional.ofNullable(entityManager.find(Role.class, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> findAll() {
        return entityManager.createQuery(findAllQuery, Role.class).getResultList();
    }

    @Override
    public void deleteById(Long id) {
        Role entity = entityManager.find(Role.class, id);
        if (entity != null) {
            entityManager.remove(entity);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Role> findByName(String name) {
        List<Role> roles = entityManager
                .createQuery(findByNameQuery, Role.class)
                .setParameter("name", name == null ? null : name.trim().toUpperCase())
                .setMaxResults(1)
                .getResultList();
        return roles.stream().findFirst();
    }
}

