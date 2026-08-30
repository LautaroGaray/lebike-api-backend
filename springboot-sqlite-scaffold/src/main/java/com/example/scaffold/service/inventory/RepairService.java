package com.example.scaffold.service.inventory;

import com.example.scaffold.domain.Audits.RepairAudit;
import com.example.scaffold.domain.auths.Users;
import com.example.scaffold.domain.inventory.Article;
import com.example.scaffold.domain.inventory.Repair;
import com.example.scaffold.domain.inventory.Warehouse;
import com.example.scaffold.dto.inventory.RepairAuditResponseDTO;
import com.example.scaffold.dto.inventory.RepairRequestDTO;
import com.example.scaffold.dto.inventory.RepairResponseDTO;
import com.example.scaffold.repository.RepairAuditRepository;
import com.example.scaffold.repository.RepairRepository;
import com.example.scaffold.repository.StatusRepository;
import com.example.scaffold.repository.UserRepository;
import com.example.scaffold.repository.WarehouseRepository;
import com.example.scaffold.repository.ArticleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class RepairService {

    public static final int DELETED_STATUS = 0;
    private static final String ACTION_CREATE = "CREATE";
    private static final String ACTION_UPDATE = "UPDATE";
    private static final String ACTION_DELETE = "DELETE";

    private final RepairRepository repairRepository;
    private final RepairAuditRepository repairAuditRepository;
    private final UserRepository userRepository;
    private final WarehouseRepository warehouseRepository;
    private final ArticleRepository articleRepository;
    private final StatusRepository statusRepository;

    public RepairService(RepairRepository repairRepository,
                         RepairAuditRepository repairAuditRepository,
                         UserRepository userRepository,
                         WarehouseRepository warehouseRepository,
                         ArticleRepository articleRepository,
                         StatusRepository statusRepository) {
        this.repairRepository = repairRepository;
        this.repairAuditRepository = repairAuditRepository;
        this.userRepository = userRepository;
        this.warehouseRepository = warehouseRepository;
        this.articleRepository = articleRepository;
        this.statusRepository = statusRepository;
    }

    public RepairResponseDTO create(RepairRequestDTO request, Long requesterId) {
        validateRequest(request);

        Users user = userRepository.findById(requesterId)
                .orElseThrow(() -> new IllegalArgumentException("Requester user not found"));
        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found"));
        if (!warehouse.isActive()) {
            throw new IllegalArgumentException("Warehouse is inactive");
        }

        Article article = null;
        if (request.getArticleId() != null) {
            article = articleRepository.findById(request.getArticleId())
                    .orElseThrow(() -> new IllegalArgumentException("Article not found"));
        }

        Repair repair = new Repair();
        repair.setStatus(request.getStatus());
        repair.setDescription(request.getDescription());
        repair.setPrice(request.getPrice());
        repair.setWarehouse(warehouse);
        repair.setArticle(article);
        repair.setUser(user);
        repair.setCreationDate(LocalDateTime.now());

        Repair saved = repairRepository.save(repair);
        saveAudit(saved, ACTION_CREATE);
        return toDto(saved);
    }

    public RepairResponseDTO update(Long repairId, RepairRequestDTO request, Long requesterId) {
        if (repairId == null) throw new IllegalArgumentException("repairId is required");
        validateRequest(request);

        Repair repair = repairRepository.findById(repairId)
                .orElseThrow(() -> new IllegalArgumentException("Repair not found"));

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found"));
        if (!warehouse.isActive()) {
            throw new IllegalArgumentException("Warehouse is inactive");
        }

        Article article = null;
        if (request.getArticleId() != null) {
            article = articleRepository.findById(request.getArticleId())
                    .orElseThrow(() -> new IllegalArgumentException("Article not found"));
        }

        repair.setStatus(request.getStatus());
        repair.setDescription(request.getDescription());
        repair.setPrice(request.getPrice());
        repair.setWarehouse(warehouse);
        repair.setArticle(article);
        repair.setEditDate(LocalDateTime.now());

        Repair updated = repairRepository.save(repair);
        saveAuditWithRequester(updated, ACTION_UPDATE, requesterId);
        return toDto(updated);
    }

    public void delete(Long repairId, Long requesterId) {
        Repair repair = repairRepository.findById(repairId)
                .orElseThrow(() -> new IllegalArgumentException("Repair not found"));

        // Log deletion before removing from DB
        RepairAudit audit = new RepairAudit();
        audit.setRepairId(repair.getId());
        audit.setPrice(repair.getPrice());
        audit.setWarehouseId(repair.getWarehouse() != null ? repair.getWarehouse().getId() : null);
        audit.setArticleId(repair.getArticle() != null ? repair.getArticle().getId() : null);
        audit.setUserId(requesterId);
        audit.setStatus(DELETED_STATUS);
        audit.setDescription("Repair deleted");
        audit.setActionType(ACTION_DELETE);
        audit.setCreationDate(LocalDateTime.now());
        repairAuditRepository.save(audit);

        repairRepository.deleteById(repairId);
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public List<RepairResponseDTO> findAll() {
        return mapToDtoList(repairRepository.findAllOrderByIdDesc());
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public List<RepairResponseDTO> findByWarehouse(String warehouseCode) {
        return mapToDtoList(repairRepository.findByWarehouseCodeOrderByIdDesc(warehouseCode));
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public List<RepairAuditResponseDTO> findHistory(Long repairId) {
        List<RepairAudit> audits = repairAuditRepository.findByRepairIdOrderByCreationDateDesc(repairId);
        List<RepairAuditResponseDTO> result = new ArrayList<>();
        for (RepairAudit audit : audits) {
            if (audit != null) result.add(toAuditDto(audit));
        }
        return result;
    }

    private void validateRequest(RepairRequestDTO request) {
        if (request == null) throw new IllegalArgumentException("Request body is required");
        if (request.getWarehouseId() == null) throw new IllegalArgumentException("warehouseId is required");
        if (request.getPrice() == null) throw new IllegalArgumentException("price is required");
        if (request.getStatus() == null) throw new IllegalArgumentException("status is required");
        if (request.getStatus() == DELETED_STATUS) throw new IllegalArgumentException("Status 0 is reserved for system use");
        if (!statusRepository.existsByStatus(request.getStatus())) {
            throw new IllegalArgumentException("Status code does not exist in catalog");
        }
    }

    private void saveAudit(Repair repair, String actionType) {
        saveAuditWithRequester(repair, actionType, repair.getUser() != null ? repair.getUser().getId() : null);
    }

    private void saveAuditWithRequester(Repair repair, String actionType, Long requesterId) {
        RepairAudit audit = new RepairAudit();
        audit.setRepairId(repair.getId());
        audit.setPrice(repair.getPrice());
        audit.setWarehouseId(repair.getWarehouse() != null ? repair.getWarehouse().getId() : null);
        audit.setArticleId(repair.getArticle() != null ? repair.getArticle().getId() : null);
        audit.setUserId(requesterId);
        audit.setStatus(repair.getStatus());
        audit.setDescription(repair.getDescription());
        audit.setActionType(actionType);
        audit.setCreationDate(LocalDateTime.now());
        repairAuditRepository.save(audit);
    }


    private void saveAuditForDelete(Repair repair, Long requesterId) {
        RepairAudit audit = new RepairAudit();
        audit.setRepairId(repair.getId());
        audit.setPrice(repair.getPrice());
        audit.setWarehouseId(repair.getWarehouse() != null ? repair.getWarehouse().getId() : null);
        audit.setArticleId(repair.getArticle() != null ? repair.getArticle().getId() : null);
        audit.setUserId(requesterId);
        audit.setStatus(DELETED_STATUS);
        audit.setDescription("Repair deleted");
        audit.setActionType(ACTION_DELETE);
        audit.setCreationDate(LocalDateTime.now());
        repairAuditRepository.save(audit);
    }

    private List<RepairResponseDTO> mapToDtoList(List<Repair> repairs) {
        List<RepairResponseDTO> result = new ArrayList<>();
        for (Repair r : repairs) {
            if (r != null) result.add(toDto(r));
        }
        return result;
    }

    private RepairResponseDTO toDto(Repair repair) {
        RepairResponseDTO dto = new RepairResponseDTO();
        dto.setId(repair.getId());
        dto.setStatus(repair.getStatus());
        dto.setStatusDescription(statusRepository.findByStatus(repair.getStatus())
                .map(s -> s.getDescription()).orElse(null));
        dto.setDescription(repair.getDescription());
        dto.setPrice(repair.getPrice());

        if (repair.getWarehouse() != null) {
            dto.setWarehouseId(repair.getWarehouse().getId());
            dto.setWarehouseCode(repair.getWarehouse().getCode());
            dto.setWarehouseName(repair.getWarehouse().getName());
        }

        if (repair.getArticle() != null) {
            dto.setArticleId(repair.getArticle().getId());
            dto.setArticleSku(repair.getArticle().getSku());
            dto.setArticleName(repair.getArticle().getName());
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
        dto.setStatus(audit.getStatus());
        dto.setStatusDescription(audit.getStatus() == null ? null :
                statusRepository.findByStatus(audit.getStatus()).map(s -> s.getDescription()).orElse(null));
        dto.setDescription(audit.getDescription());
        dto.setActionType(audit.getActionType());
        dto.setPrice(audit.getPrice());
        dto.setWarehouseId(audit.getWarehouseId());
        dto.setArticleId(audit.getArticleId());
        dto.setUserId(audit.getUserId());
        dto.setCreationDate(audit.getCreationDate());
        return dto;
    }
}

