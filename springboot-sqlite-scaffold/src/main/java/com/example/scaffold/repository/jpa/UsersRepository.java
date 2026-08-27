package com.example.scaffold.repository.jpa;

import com.example.scaffold.domain.auths.Users;
import com.example.scaffold.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public class UsersRepository implements UserRepository {
    @Value("${repository.query.users.findAll}")
    private String findAllQuery;

    @Value("${repository.query.users.findByEmail}")
    private String findByEmailQuery;

    @Value("${repository.query.users.findByUsername}")
    private String findByUsernameQuery;


    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Users save(Users entity) {
        if (entity.getId() == null) {
            entityManager.persist(entity);
            return entity;
        }
        return entityManager.merge(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Users> findById(Long id) {
        return Optional.ofNullable(entityManager.find(Users.class, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Users> findAll() {
        return entityManager
                .createQuery(findAllQuery, Users.class)
                .getResultList();
    }

    @Override
    public void deleteById(Long id) {
        Users entity = entityManager.find(Users.class, id);
        if (entity != null) {
            entityManager.remove(entity);
        }
    }

    @Transactional(readOnly = true)
    public Optional<Users> findByEmail(String email) {
        List<Users> users = entityManager
                .createQuery(findByEmailQuery, Users.class)
                .setParameter("email", email)
                .setMaxResults(1)
                .getResultList();
        return users.stream().findFirst();
    }

    @Transactional(readOnly = true)
    public Optional<Users> findByUsername(String username) {
        List<Users> users = entityManager
                .createQuery(findByUsernameQuery, Users.class)
                .setParameter("username", username)
                .setMaxResults(1)
                .getResultList();
        return users.stream().findFirst();
    }

}
