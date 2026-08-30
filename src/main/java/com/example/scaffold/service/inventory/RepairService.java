package com.example.scaffold.service.inventory;

import com.example.scaffold.domain.Audits.RepairAudit;
import com.example.scaffold.domain.auths.Users;
import com.example.scaffold.domain.inventory.Article;
import com.example.scaffold.domain.inventory.Repair;
import com.example.scaffold.domain.inventory.Warehouse;
import com.example.scaffold.dto.inventory.RepairArticleDTO;
import com.example.scaffold.dto.inventory.RepairAuditResponseDTO;
import com.example.scaffold.dto.inventory.RepairRequestDTO;
import com.example.scaffold.dto.inventory.RepairResponseDTO;
import com.example.scaffold.repository.ArticleRepository;
import com.example.scaffold.repository.RepairAuditRepository;
import com.example.scaffold.repository.RepairRepository;
import com.example.scaffold.repository.WarehouseRepository;
import com.example.scaffold.service.auths.DocumentWarehouseScopeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class RepairService {

    private static final String ACTION_CREATE = "CREATE";
    private static final String ACTION_UPDATE = "UPDATE";
    private static final String ACTION_DELETE = "DELETE";

    private final RepairRepository repairRepository;
    private final RepairAuditRepository repairAuditRepository;
    private final WarehouseRepository warehouseRepository;
    private final ArticleRepository articleRepository;
    private final DocumentWarehouseScopeService documentWarehouseScopeService;

    public RepairService(RepairRepository repairRepository,
                         RepairAuditRepository repairAuditRepository,
                         WarehouseRepository warehouseRepository,
                         ArticleRepository articleRepository,
                         DocumentWarehouseScopeService documentWarehouseScopeService) {
        this.repairRepository = repairRepository;
        this.repairAuditRepository = repairAuditRepository;
        this.warehouseRepository = warehouseRepository;
        this.articleRepository = articleRepository;
        this.documentWarehouseScopeService = documentWarehouseScopeService;
    }

    public RepairResponseDTO create(RepairRequestDTO request, Long requesterId) {
        validateRequest(request);

        Users requester = documentWarehouseScopeService.getRequesterOrThrow(requesterId);
        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found"));
        if (!warehouse.isActive()) {
            throw new IllegalArgumentException("Warehouse is inactive");
        }
        documentWarehouseScopeService.assertCanAccessWarehouse(requester, warehouse.getCode(), "Repair");

        List<Article> articles = resolveArticles(request.getArticleIds());

        Repair repair = new Repair();
        repair.setDescription(request.getDescription());
        repair.setPrice(request.getPrice());
        repair.setWarehouse(warehouse);
        repair.setUser(requester);
        repair.setCreationDate(LocalDateTime.now());
        repair.getArticles().clear();
        repair.getArticles().addAll(articles);

        Repair saved = repairRepository.save(repair);
        saveAuditWithRequester(saved, ACTION_CREATE, requesterId, saved.getDescription());
        return toDto(saved);
    }

    public RepairResponseDTO update(Long repairId, RepairRequestDTO request, Long requesterId) {
        if (repairId == null) {
            throw new IllegalArgumentException("repairId is required");
        }
        validateRequest(request);

        Users requester = documentWarehouseScopeService.getRequesterOrThrow(requesterId);
        Repair repair = repairRepository.findById(repairId)
                .orElseThrow(() -> new IllegalArgumentException("Repair not found"));

        documentWarehouseScopeService.assertCanAccessWarehouse(requester,
                repair.getWarehouse() != null ? repair.getWarehouse().getCode() : null,
                "Repair");

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found"));
        if (!warehouse.isActive()) {
            throw new IllegalArgumentException("Warehouse is inactive");
        }
        documentWarehouseScopeService.assertCanAccessWarehouse(requester, warehouse.getCode(), "Repair");

        List<Article> articles = resolveArticles(request.getArticleIds());

        repair.setDescription(request.getDescription());
        repair.setPrice(request.getPrice());
        repair.setWarehouse(warehouse);
        repair.setEditDate(LocalDateTime.now());
        repair.getArticles().clear();
        repair.getArticles().addAll(articles);

        Repair updated = repairRepository.save(repair);
        saveAuditWithRequester(updated, ACTION_UPDATE, requesterId, updated.getDescription());
        return toDto(updated);
    }

    public void delete(Long repairId, Long requesterId) {
        Users requester = documentWarehouseScopeService.getRequesterOrThrow(requesterId);
        Repair repair = repairRepository.findById(repairId)
                .orElseThrow(() -> new IllegalArgumentException("Repair not found"));

        documentWarehouseScopeService.assertCanAccessWarehouse(requester,
                repair.getWarehouse() != null ? repair.getWarehouse().getCode() : null,
                "Repair");

        saveAuditWithRequester(repair, ACTION_DELETE, requesterId, "Repair deleted");
        repairRepository.deleteById(repairId);
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public List<RepairResponseDTO> findAll(Long requesterId) {
        Users requester = documentWarehouseScopeService.getRequesterOrThrow(requesterId);
        List<Repair> repairs = repairRepository.findAllOrderByIdDesc();
        return mapToDtoList(filterByScope(requester, repairs));
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public List<RepairResponseDTO> findByWarehouse(String warehouseCode, Long requesterId) {
        Users requester = documentWarehouseScopeService.getRequesterOrThrow(requesterId);
        String normalizedCode = normalizeWarehouseCode(warehouseCode);
        documentWarehouseScopeService.assertCanAccessWarehouse(requester, normalizedCode, "Repair");
        return mapToDtoList(repairRepository.findByWarehouseCodeOrderByIdDesc(normalizedCode));
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public List<RepairAuditResponseDTO> findHistory(Long repairId, Long requesterId) {
        Users requester = documentWarehouseScopeService.getRequesterOrThrow(requesterId);
        documentWarehouseScopeService.assertCanViewHistory(requester, "Repair");

        Repair repair = repairRepository.findById(repairId)
                .orElseThrow(() -> new IllegalArgumentException("Repair not found"));
        documentWarehouseScopeService.assertCanAccessWarehouse(requester,
                repair.getWarehouse() != null ? repair.getWarehouse().getCode() : null,
                "Repair");

        List<RepairAudit> audits = repairAuditRepository.findByRepairIdOrderByCreationDateDesc(repairId);
        List<RepairAuditResponseDTO> result = new ArrayList<>();
        for (RepairAudit audit : audits) {
            if (audit != null) {
                result.add(toAuditDto(audit));
            }
        }
        return result;
    }

    private void validateRequest(RepairRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (request.getWarehouseId() == null) {
            throw new IllegalArgumentException("warehouseId is required");
        }
        if (request.getPrice() == null) {
            throw new IllegalArgumentException("price is required");
        }
    }

    private List<Article> resolveArticles(List<Long> articleIds) {
        List<Article> articles = new ArrayList<>();
        if (articleIds == null || articleIds.isEmpty()) {
            return articles;
        }

        List<Long> uniqueArticleIds = new ArrayList<>();
        for (Long articleId : articleIds) {
            if (articleId == null) {
                throw new IllegalArgumentException("articleIds cannot contain null values");
            }
            if (!uniqueArticleIds.contains(articleId)) {
                uniqueArticleIds.add(articleId);
            }
        }

        for (Long articleId : uniqueArticleIds) {
            Article article = articleRepository.findById(articleId)
                    .orElseThrow(() -> new IllegalArgumentException("Article not found"));
            articles.add(article);
        }
        return articles;
    }

    private void saveAuditWithRequester(Repair repair, String actionType, Long requesterId, String description) {
        RepairAudit audit = new RepairAudit();
        audit.setRepairId(repair.getId());
        audit.setPrice(repair.getPrice());
        audit.setWarehouseId(repair.getWarehouse() != null ? repair.getWarehouse().getId() : null);
        audit.setArticleIds(serializeArticleIds(repair.getArticles()));
        audit.setUserId(requesterId);
        audit.setDescription(description);
        audit.setActionType(actionType);
        audit.setCreationDate(LocalDateTime.now());
        repairAuditRepository.save(audit);
    }

    private List<Repair> filterByScope(Users requester, List<Repair> repairs) {
        List<Repair> filtered = new ArrayList<>();
        for (Repair repair : repairs) {
            if (repair == null) {
                continue;
            }
            String warehouseCode = repair.getWarehouse() != null ? repair.getWarehouse().getCode() : null;
            if (documentWarehouseScopeService.canAccessWarehouse(requester, warehouseCode)) {
                filtered.add(repair);
            }
        }
        return filtered;
    }

    private List<RepairResponseDTO> mapToDtoList(List<Repair> repairs) {
        List<RepairResponseDTO> result = new ArrayList<>();
        for (Repair repair : repairs) {
            if (repair != null) {
                result.add(toDto(repair));
            }
        }
        return result;
    }

    private RepairResponseDTO toDto(Repair repair) {
        RepairResponseDTO dto = new RepairResponseDTO();
        dto.setId(repair.getId());
        dto.setDescription(repair.getDescription());
        dto.setPrice(repair.getPrice());

        if (repair.getWarehouse() != null) {
            dto.setWarehouseId(repair.getWarehouse().getId());
            dto.setWarehouseCode(repair.getWarehouse().getCode());
            dto.setWarehouseName(repair.getWarehouse().getName());
        }

        if (repair.getArticles() != null) {
            for (Article article : repair.getArticles()) {
                if (article != null) {
                    dto.getArticles().add(toRepairArticleDto(article));
                }
            }
        }

        if (repair.getUser() != null) {
            dto.setUserId(repair.getUser().getId());
            dto.setUsername(repair.getUser().getUsername());
            dto.setUserEmail(repair.getUser().getEmail());
        }

        dto.setCreationDate(repair.getCreationDate());
        dto.setEditDate(repair.getEditDate());
        return dto;
    }

    private RepairAuditResponseDTO toAuditDto(RepairAudit audit) {
        RepairAuditResponseDTO dto = new RepairAuditResponseDTO();
        dto.setId(audit.getId());
        dto.setRepairId(audit.getRepairId());
        dto.setDescription(audit.getDescription());
        dto.setActionType(audit.getActionType());
        dto.setPrice(audit.getPrice());
        dto.setWarehouseId(audit.getWarehouseId());
        dto.setArticleIds(parseArticleIds(audit.getArticleIds()));
        dto.setUserId(audit.getUserId());
        dto.setCreationDate(audit.getCreationDate());
        return dto;
    }

    private RepairArticleDTO toRepairArticleDto(Article article) {
        RepairArticleDTO dto = new RepairArticleDTO();
        dto.setId(article.getId());
        dto.setSku(article.getSku());
        dto.setName(article.getName());
        return dto;
    }

    private String serializeArticleIds(List<Article> articles) {
        if (articles == null || articles.isEmpty()) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        for (Article article : articles) {
            if (article == null || article.getId() == null) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(article.getId());
        }
        return builder.length() == 0 ? null : builder.toString();
    }

    private List<Long> parseArticleIds(String articleIds) {
        List<Long> result = new ArrayList<>();
        if (!StringUtils.hasText(articleIds)) {
            return result;
        }

        String[] values = articleIds.split(",");
        for (String value : values) {
            String trimmed = value == null ? null : value.trim();
            if (!StringUtils.hasText(trimmed)) {
                continue;
            }
            result.add(Long.valueOf(trimmed));
        }
        return result;
    }

    private String normalizeWarehouseCode(String code) {
        if (!StringUtils.hasText(code)) {
            return code;
        }
        return code.trim().replace('_', '-').toUpperCase();
    }
}
