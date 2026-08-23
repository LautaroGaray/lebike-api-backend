package com.example.scaffold.repository.jpa;

import com.example.scaffold.domain.Module;
import com.example.scaffold.repository.ModuleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public class ModulesRepository implements ModuleRepository {

    @Value("${repository.query.modules.findAll}")
    private String findAllQuery;

    @Value("${repository.query.modules.findByMainId}")
    private String findByMainIdQuery;

    @Value("${repository.query.modules.findByName:select m from Module m where m.name = :name}")
    private String findByNameQuery;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Module save(Module entity) {
        if (entity.getId() == null) {
            entityManager.persist(entity);
            return entity;
        }
        return entityManager.merge(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Module> findById(Long id) {
        return Optional.ofNullable(entityManager.find(Module.class, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Module> findAll() {
        return entityManager.createQuery(findAllQuery, Module.class).getResultList();
    }

    @Override
    public void deleteById(Long id) {
        Module entity = entityManager.find(Module.class, id);
        if (entity != null) {
            entityManager.remove(entity);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Module> findByMainId(String mainId) {
        List<Module> modules = entityManager
                .createQuery(findByMainIdQuery, Module.class)
                .setParameter("mainId", mainId)
                .setMaxResults(1)
                .getResultList();
        return modules.stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Module> findByName(String name) {
        List<Module> modules = entityManager
                .createQuery(findByNameQuery, Module.class)
                .setParameter("name", name)
                .setMaxResults(1)
                .getResultList();
        return modules.stream().findFirst();
    }
}

