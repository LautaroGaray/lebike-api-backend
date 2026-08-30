package com.example.scaffold.service.inventory;

import com.example.scaffold.domain.documents.DocumentsEnum;
import com.example.scaffold.domain.inventory.Article;
import com.example.scaffold.dto.inventory.ArticleRequestDTO;
import com.example.scaffold.dto.inventory.ArticleResponseDTO;
import com.example.scaffold.repository.ArticleRepository;
import com.example.scaffold.repository.ReceiptRepository;
import com.example.scaffold.util.KeyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Objects;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class ArticleService {

    private static final int RECEIVED_STATUS = 110;

    private final ArticleRepository articleRepository;
    private final ReceiptRepository receiptRepository;
    private final KeyService keyService;

    public ArticleService(ArticleRepository articleRepository, ReceiptRepository receiptRepository, KeyService keyService) {
        this.articleRepository = articleRepository;
        this.receiptRepository = receiptRepository;
        this.keyService = keyService;
    }

    public ArticleResponseDTO findByCriteria(boolean externalSku, ArticleRequestDTO request){
        if(externalSku && !StringUtils.hasText(request.getExternalSku())){
            throw  new IllegalArgumentException("externalSku is required");
        }else if(!externalSku && !StringUtils.hasText(request.getSku())){
            throw new IllegalArgumentException("sku is required");
        }

        String normalizedSku = externalSku ? request.getExternalSku().trim().toUpperCase(): request.getSku().trim().toUpperCase();

        Article article = externalSku ? articleRepository.findByExternalSku(normalizedSku).orElse(null) : articleRepository.findBySku(normalizedSku).orElse(null);
        if(article == null) {
            throw new IllegalArgumentException("Article not found");
        }

        return toDto(article);
    }
    public ArticleResponseDTO create(ArticleRequestDTO request) {
        validateCreateRequest(request);

        String sku = StringUtils.hasText(request.getSku()) ? request.getSku() : keyService.getKey(DocumentsEnum.ARTICLE, null).getCompletKey();
        String normalizedSku = request.getSku().trim().toUpperCase();
        if (articleRepository.findBySku(normalizedSku).isPresent()) {
            throw new IllegalArgumentException("Article with this sku already exists");
        }

        Article article = new Article();
        article.setSku(normalizedSku);
        article.setExternalSku(normalizeNullableText(request.getExternalSku()));
        article.setName(request.getName().trim());
        article.setType(normalizeNullableText(request.getType()));
        article.setSupplier(normalizeNullableText(request.getSupplier()));
        article.setPurchasePrice(request.getPurchasePrice());
        article.setSalePrice(request.getSalePrice());

        return toDto(articleRepository.save(article));
    }

    public ArticleResponseDTO update(Long articleId, ArticleRequestDTO request) {
        if (articleId == null) {
            throw new IllegalArgumentException("articleId is required");
        }
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }

        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new IllegalArgumentException("Article not found"));

        if (StringUtils.hasText(request.getSku())) {
            String normalizedSku = request.getSku().trim().toUpperCase();
            Article existingBySku = articleRepository.findBySku(normalizedSku).orElse(null);
            if (existingBySku != null && !Objects.equals(existingBySku.getId(), article.getId())) {
                throw new IllegalArgumentException("Article with this sku already exists");
            }
            article.setSku(normalizedSku);
        }

        if(StringUtils.hasText(request.getExternalSku())) {
            String normalizedExternalSku = request.getExternalSku().trim().toUpperCase();
            Article existingByExternalSku = articleRepository.findByExternalSku(normalizedExternalSku).orElse(null);
            if (existingByExternalSku != null && !Objects.equals(existingByExternalSku.getId(), article.getId())) {
                throw new IllegalArgumentException("Article with this external sku already exists");
            }
            article.setExternalSku(normalizedExternalSku);
        }

        if (StringUtils.hasText(request.getName())) {
            article.setName(request.getName().trim());
        }
        if (request.getType() != null) {
            article.setType(normalizeNullableText(request.getType()));
        }
        if (request.getSupplier() != null) {
            article.setSupplier(normalizeNullableText(request.getSupplier()));
        }
        if (request.getPurchasePrice() != null) {
            article.setPurchasePrice(request.getPurchasePrice());
        }
        if (request.getSalePrice() != null) {
            article.setSalePrice(request.getSalePrice());
        }

        return toDto(articleRepository.save(article));
    }

    public void delete(Long articleId) {
        if (articleId == null) {
            throw new IllegalArgumentException("articleId is required");
        }

        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new IllegalArgumentException("Article not found"));

        boolean linkedToOpenReceipts = receiptRepository.existsByArticleIdAndStatusLessThan(articleId, RECEIVED_STATUS);
        if (linkedToOpenReceipts) {
            throw new IllegalArgumentException("Cannot delete article: it is referenced by receipts not yet received");
        }

        articleRepository.deleteById(article.getId());
    }

    private void validateCreateRequest(ArticleRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("name is required");
        }
        if (request.getPurchasePrice() == null) {
            throw new IllegalArgumentException("purchasePrice is required");
        }
        if (request.getSalePrice() == null) {
            throw new IllegalArgumentException("salePrice is required");
        }
    }

    private String normalizeNullableText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private ArticleResponseDTO toDto(Article article) {
        ArticleResponseDTO dto = new ArticleResponseDTO();
        dto.setId(article.getId());
        dto.setSku(article.getSku());
        dto.setExternalSku(article.getExternalSku());
        dto.setName(article.getName());
        dto.setType(article.getType());
        dto.setSupplier(article.getSupplier());
        dto.setPurchasePrice(article.getPurchasePrice());
        dto.setSalePrice(article.getSalePrice());
        return dto;
    }
}

