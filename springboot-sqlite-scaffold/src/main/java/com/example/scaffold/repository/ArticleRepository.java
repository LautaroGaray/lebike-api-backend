package com.example.scaffold.repository;

import com.example.scaffold.domain.inventory.Article;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {
	Optional<Article> findBySku(String sku);
	Optional<Article> findByExternalSku(String externalSku);
}
