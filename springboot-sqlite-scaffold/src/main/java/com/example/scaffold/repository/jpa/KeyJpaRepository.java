package com.example.scaffold.repository.jpa;

import com.example.scaffold.domain.Audits.Keys;
import com.example.scaffold.repository.KeyRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public class KeyJpaRepository implements KeyRepository {

	@Value("${repository.query.key.findAll}")
	private String findAllQuery;

	@Value("${repository.query.key.findByTargetDestiny}")
	private String findByTargetDestinyQuery;

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public Keys save(Keys entity) {
		if (entity.getId() == null) {
			entityManager.persist(entity);
			return entity;
		}
		return entityManager.merge(entity);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Keys> findById(Long id) {
		return Optional.ofNullable(entityManager.find(Keys.class, id));
	}

	@Override
	@Transactional(readOnly = true)
	public List<Keys> findAll() {
		return entityManager.createQuery(findAllQuery, Keys.class).getResultList();
	}

	@Override
	public void deleteById(Long id) {
		Keys entity = entityManager.find(Keys.class, id);
		if (entity != null) {
			entityManager.remove(entity);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Keys> findByTargetDestiny(String targetDestiny) {
		List<Keys> result = entityManager.createQuery(findByTargetDestinyQuery, Keys.class)
				.setParameter("targetDestiny", targetDestiny)
				.setMaxResults(1)
				.getResultList();
		return result.stream().findFirst();
	}

	@Override
	public Optional<Keys> findByTargetDestinyForUpdate(String targetDestiny) {
		List<Keys> result = entityManager.createQuery(findByTargetDestinyQuery, Keys.class)
				.setParameter("targetDestiny", targetDestiny)
				.setLockMode(LockModeType.PESSIMISTIC_WRITE)
				.setMaxResults(1)
				.getResultList();
		return result.stream().findFirst();
	}
}

