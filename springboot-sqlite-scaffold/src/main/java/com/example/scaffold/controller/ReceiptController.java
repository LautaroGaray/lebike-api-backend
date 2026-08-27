package com.example.scaffold.controller;

import com.example.scaffold.domain.auths.Permissions;
import com.example.scaffold.domain.auths.Role;
import com.example.scaffold.domain.auths.RolePermissions;
import com.example.scaffold.domain.auths.Users;
import com.example.scaffold.domain.context.Module;
import com.example.scaffold.domain.inventory.Article;
import com.example.scaffold.domain.inventory.Warehouse;
import com.example.scaffold.dto.ResponseData;
import com.example.scaffold.dto.document.ReceiptCreateRequestDTO;
import com.example.scaffold.dto.document.ReceiptDetailCreateRequestDTO;
import com.example.scaffold.dto.document.ReceiptResponseDTO;
import com.example.scaffold.dto.document.ReceiptStatusLogResponseDTO;
import com.example.scaffold.repository.ArticleRepository;
import com.example.scaffold.repository.ModuleRepository;
import com.example.scaffold.repository.PermissionRepository;
import com.example.scaffold.repository.ReceiptRepository;
import com.example.scaffold.repository.RolePermissionsRepository;
import com.example.scaffold.repository.StatusRepository;
import com.example.scaffold.repository.UserRepository;
import com.example.scaffold.repository.WarehouseRepository;
import com.example.scaffold.security.BearerTokenInterceptor;
import com.example.scaffold.service.auths.WarehouseAccessPolicyService;
import com.example.scaffold.service.document.ReceiptService;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/receipts")
public class ReceiptController {

    private static final String RECEIPTS_MODULE = "Receipts";
    private static final String WRITE_PERMISSION = "WRITE";
    private static final String READ_PERMISSION = "READ";
    private static final String POINT_OF_SALE_PREFIX = "P-";

    private final ReceiptService receiptService;
    private final ReceiptRepository receiptRepository;
    private final UserRepository userRepository;
    private final WarehouseRepository warehouseRepository;
    private final ArticleRepository articleRepository;
    private final ModuleRepository moduleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionsRepository rolePermissionsRepository;
    private final StatusRepository statusRepository;
    private final WarehouseAccessPolicyService warehouseAccessPolicyService;

    public ReceiptController(ReceiptService receiptService,
                             ReceiptRepository receiptRepository,
                             UserRepository userRepository,
                             WarehouseRepository warehouseRepository,
                             ArticleRepository articleRepository,
                             ModuleRepository moduleRepository,
                             PermissionRepository permissionRepository,
                              RolePermissionsRepository rolePermissionsRepository,
                               StatusRepository statusRepository,
                               WarehouseAccessPolicyService warehouseAccessPolicyService) {
        this.receiptService = receiptService;
        this.receiptRepository = receiptRepository;
        this.userRepository = userRepository;
        this.warehouseRepository = warehouseRepository;
        this.articleRepository = articleRepository;
        this.moduleRepository = moduleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionsRepository = rolePermissionsRepository;
        this.statusRepository = statusRepository;
        this.warehouseAccessPolicyService = warehouseAccessPolicyService;
    }

    @PostMapping("/register")
    public ResponseEntity<ResponseData> register(@RequestBody ReceiptCreateRequestDTO request) {
        if (!hasRequesterPermission(WRITE_PERMISSION)) {
            return ResponseEntity.status(403).body(new ResponseData(null, false, "Insufficient permissions to create receipts"));
        }

        ResponseEntity<ResponseData> validationError = validateCreateRequest(request);
        if (validationError != null) {
            return validationError;
        }

        try {
            ReceiptResponseDTO created = receiptService.createReceiptWithDetails(request);
            return ResponseEntity.ok(new ResponseData(created, true, "Receipt created successfully"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ResponseData(null, false, ex.getMessage()));
        }
    }

    @PutMapping("/edit/{receiptId}")
    public ResponseEntity<ResponseData> edit(@PathVariable Long receiptId,
                                             @RequestBody ReceiptCreateRequestDTO request) {
        if (!isRequesterAdmin()) {
            return ResponseEntity.status(403).body(new ResponseData(null, false, "Only ADMIN can modify receipts"));
        }

        if (!hasRequesterPermission(WRITE_PERMISSION)) {
            return ResponseEntity.status(403).body(new ResponseData(null, false, "Insufficient permissions to update receipts"));
        }

        if (receiptId == null) {
            return ResponseEntity.badRequest().body(new ResponseData(null, false, "receiptId is required"));
        }

        com.example.scaffold.domain.documents.Receipt existing = receiptRepository.findById(receiptId).orElse(null);
        if (existing == null) {
            return ResponseEntity.status(404).body(new ResponseData(null, false, "Receipt not found"));
        }

        Users requesterUser = getRequesterUser();
        if (requesterUser == null) {
            return ResponseEntity.status(403).body(new ResponseData(null, false, "Unable to determine requester user"));
        }

        String accessError = warehouseAccessPolicyService
                .validateReceiptWarehouseAccess(requesterUser, existing.getOrigin(), existing.getDestiny())
                .orElse(null);
        if (accessError != null) {
            return ResponseEntity.status(403).body(new ResponseData(null, false, accessError));
        }

        ResponseEntity<ResponseData> validationError = validateCreateRequest(request);
        if (validationError != null) {
            return validationError;
        }

        try {
            ReceiptResponseDTO updated = receiptService.updateReceiptWithDetails(receiptId, request);
            return ResponseEntity.ok(new ResponseData(updated, true, "Receipt updated successfully"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ResponseData(null, false, ex.getMessage()));
        }
    }

    @DeleteMapping("/delete/{receiptId}")
    public ResponseEntity<ResponseData> delete(@PathVariable Long receiptId) {
        if (!isRequesterAdmin()) {
            return ResponseEntity.status(403).body(new ResponseData(null, false, "Only ADMIN can delete receipts"));
        }

        if (!hasRequesterPermission(WRITE_PERMISSION)) {
            return ResponseEntity.status(403).body(new ResponseData(null, false, "Insufficient permissions to delete receipts"));
        }

        if (receiptId == null) {
            return ResponseEntity.badRequest().body(new ResponseData(null, false, "receiptId is required"));
        }

        com.example.scaffold.domain.documents.Receipt existing = receiptRepository.findById(receiptId).orElse(null);
        if (existing == null) {
            return ResponseEntity.status(404).body(new ResponseData(null, false, "Receipt not found"));
        }

        Users requesterUser = getRequesterUser();
        if (requesterUser == null) {
            return ResponseEntity.status(403).body(new ResponseData(null, false, "Unable to determine requester user"));
        }

        String accessError = warehouseAccessPolicyService
                .validateReceiptWarehouseAccess(requesterUser, existing.getOrigin(), existing.getDestiny())
                .orElse(null);
        if (accessError != null) {
            return ResponseEntity.status(403).body(new ResponseData(null, false, accessError));
        }

        try {
            receiptService.deleteReceipt(receiptId, requesterUser.getId());
            return ResponseEntity.ok(new ResponseData(null, true, "Receipt deleted successfully"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(new ResponseData(null, false, ex.getMessage()));
        }
    }

    @GetMapping("/findByUserAndWarehouse")
    public ResponseEntity<ResponseData> findByUserAndWarehouse(@RequestParam Long userId,
                                                               @RequestParam String warehouseCode) {
        if (!hasRequesterPermission(READ_PERMISSION)) {
            return ResponseEntity.status(403).body(new ResponseData(null, false, "Insufficient permissions to read receipts"));
        }

        if (userId == null || !userRepository.findById(userId).isPresent()) {
            return ResponseEntity.status(404).body(new ResponseData(null, false, "User not found"));
        }

        Users requesterUser = getRequesterUser();
        if (requesterUser == null) {
            return ResponseEntity.status(403).body(new ResponseData(null, false, "Unable to determine requester user"));
        }

        if (!StringUtils.hasText(warehouseCode)) {
            return ResponseEntity.badRequest().body(new ResponseData(null, false, "warehouseCode is required"));
        }

        String normalizedWarehouseCode = warehouseCode.trim();
        if (!isPointOfSaleCode(normalizedWarehouseCode) && !warehouseExistsAndActive(normalizedWarehouseCode)) {
            return ResponseEntity.status(404).body(new ResponseData(null, false, "Warehouse does not exist or is inactive"));
        }

        String warehouseAccessError = warehouseAccessPolicyService
                .validateSingleWarehouseAccess(requesterUser, normalizedWarehouseCode)
                .orElse(null);
        if (warehouseAccessError != null) {
            return ResponseEntity.status(403).body(new ResponseData(null, false, warehouseAccessError));
        }

        List<ReceiptResponseDTO> receipts = receiptService.findByUserAndWarehouse(userId, normalizedWarehouseCode);
        return ResponseEntity.ok(new ResponseData(receipts, true, "Receipts retrieved successfully"));
    }

    @GetMapping("/history/{receiptId}")
    public ResponseEntity<ResponseData> history(@PathVariable Long receiptId) {
        if (!isRequesterOwner()) {
            return ResponseEntity.status(403).body(new ResponseData(null, false, "Only OWNER can read receipt history"));
        }

        if (!hasRequesterPermission(READ_PERMISSION)) {
            return ResponseEntity.status(403).body(new ResponseData(null, false, "Insufficient permissions to read receipt history"));
        }

        if (receiptId == null) {
            return ResponseEntity.badRequest().body(new ResponseData(null, false, "receiptId is required"));
        }

        if (!receiptRepository.findById(receiptId).isPresent()) {
            return ResponseEntity.status(404).body(new ResponseData(null, false, "Receipt not found"));
        }

        List<ReceiptStatusLogResponseDTO> history = receiptService.findStatusHistory(receiptId);
        return ResponseEntity.ok(new ResponseData(history, true, "Receipt history retrieved successfully"));
    }

    @GetMapping("/findAll")
    public ResponseEntity<ResponseData> findAll() {
        if (!hasRequesterPermission(READ_PERMISSION)) {
            return ResponseEntity.status(403).body(new ResponseData(null, false, "Insufficient permissions to read receipts"));
        }

        Users requesterUser = getRequesterUser();
        if (requesterUser == null) {
            return ResponseEntity.status(403).body(new ResponseData(null, false, "Unable to determine requester user"));
        }

        String requesterRole = requesterUser.getRole() != null ? requesterUser.getRole().getName() : null;
        String roleUpper = requesterRole == null ? "" : requesterRole.trim().toUpperCase(Locale.ROOT);

        List<ReceiptResponseDTO> receipts = receiptService.findAll();

        if (!Role.OWNER.equals(roleUpper)) {
            List<String> allowedWarehouseCodes = requesterUser.getWarehousesAllowed() == null
                    ? java.util.Collections.emptyList()
                    : requesterUser.getWarehousesAllowed().stream()
                    .filter(Objects::nonNull)
                    .map(warehouse -> warehouse.getCode() == null ? null : warehouse.getCode().trim().toUpperCase(Locale.ROOT))
                    .filter(StringUtils::hasText)
                    .distinct()
                    .collect(Collectors.toList());

            if (Role.ADMIN.equals(roleUpper) && allowedWarehouseCodes.isEmpty()) {
                return ResponseEntity.ok(new ResponseData(receipts, true, "Receipts retrieved successfully"));
            }

            receipts = receipts.stream()
                    .filter(receipt -> isVisibleForWarehouses(receipt, allowedWarehouseCodes))
                    .collect(Collectors.toList());
        }

        return ResponseEntity.ok(new ResponseData(receipts, true, "Receipts retrieved successfully"));
    }

    private ResponseEntity<ResponseData> validateCreateRequest(ReceiptCreateRequestDTO request) {
        if (request == null) {
            return ResponseEntity.badRequest().body(new ResponseData(null, false, "Request body is required"));
        }

        Users requestUser = request.getUserId() == null ? null : userRepository.findById(request.getUserId()).orElse(null);
        if (requestUser == null) {
            return ResponseEntity.status(404).body(new ResponseData(null, false, "User not found"));
        }

        if (request.getStatus() == null) {
            return ResponseEntity.badRequest().body(new ResponseData(null, false, "status is required"));
        }

        if (!statusRepository.existsByStatus(request.getStatus())) {
            return ResponseEntity.status(404).body(new ResponseData(null, false, "status code does not exist in catalog"));
        }

        if (!StringUtils.hasText(request.getOrigin()) || !StringUtils.hasText(request.getDestiny())) {
            return ResponseEntity.badRequest().body(new ResponseData(null, false, "origin and destiny are required"));
        }

        String normalizedOrigin = request.getOrigin().trim();
        String normalizedDestiny = request.getDestiny().trim();
        request.setOrigin(normalizedOrigin);
        request.setDestiny(normalizedDestiny);

        Users requesterUser = getRequesterUser();
        if (requesterUser == null) {
            return ResponseEntity.status(403).body(new ResponseData(null, false, "Unable to determine requester user"));
        }

        if (!isPointOfSaleCode(normalizedOrigin) && !warehouseExistsAndActive(normalizedOrigin)) {
            return ResponseEntity.status(404).body(new ResponseData(null, false, "Origin warehouse does not exist or is inactive"));
        }

        if (!isPointOfSaleCode(normalizedDestiny) && !warehouseExistsAndActive(normalizedDestiny)) {
            return ResponseEntity.status(404).body(new ResponseData(null, false, "Destiny warehouse does not exist or is inactive"));
        }

        String warehouseAccessError = warehouseAccessPolicyService
                .validateReceiptWarehouseAccess(requesterUser, normalizedOrigin, normalizedDestiny)
                .orElse(null);
        if (warehouseAccessError != null) {
            return ResponseEntity.status(403).body(new ResponseData(null, false, warehouseAccessError));
        }

        if (request.getDetails() == null || request.getDetails().isEmpty()) {
            return ResponseEntity.badRequest().body(new ResponseData(null, false, "details are required"));
        }

        for (ReceiptDetailCreateRequestDTO detail : request.getDetails()) {
            if (detail == null || detail.getArticleId() == null) {
                return ResponseEntity.badRequest().body(new ResponseData(null, false, "Each detail must include articleId"));
            }

            Article article = articleRepository.findById(detail.getArticleId()).orElse(null);
            if (article == null || !article.isActive()) {
                return ResponseEntity.status(404).body(new ResponseData(null, false, "Article does not exist or is inactive: " + detail.getArticleId()));
            }
        }

        return null;
    }

    private boolean hasRequesterPermission(String permissionCode) {
        Long requesterRoleId = getRequesterRoleId();
        if (requesterRoleId == null || !StringUtils.hasText(permissionCode)) {
            return false;
        }

        Module module = moduleRepository.findByName(RECEIPTS_MODULE).orElse(null);
        Permissions permission = permissionRepository.findByCode(permissionCode).orElse(null);
        if (module == null || permission == null) {
            return false;
        }

        RolePermissions rolePermission = rolePermissionsRepository
                .findByRoleIdAndModuleIdAndPermissionId(requesterRoleId, module.getId(), permission.getId())
                .orElse(null);

        return !Objects.isNull(rolePermission) && rolePermission.isEnabled();
    }

    private boolean warehouseExistsAndActive(String code) {
        Warehouse warehouse = warehouseRepository.findByCode(code).orElse(null);
        return warehouse != null && warehouse.isActive();
    }

    private boolean isPointOfSaleCode(String value) {
        return value != null && value.toUpperCase().startsWith(POINT_OF_SALE_PREFIX);
    }

    private Long getRequesterRoleId() {
        Object roleAttribute = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes() != null
                ? ((org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.getRequestAttributes())
                .getRequest().getAttribute(BearerTokenInterceptor.REQUEST_ROLE_ID_ATTRIBUTE)
                : null;

        return roleAttribute instanceof Number ? ((Number) roleAttribute).longValue() : null;
    }

    private boolean isRequesterAdmin() {
        Object roleAttribute = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes() != null
                ? ((org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.getRequestAttributes())
                .getRequest().getAttribute(BearerTokenInterceptor.REQUEST_ROLE_ATTRIBUTE)
                : null;

        String roleName = roleAttribute instanceof String ? ((String) roleAttribute).trim().toUpperCase() : null;
        return Role.ADMIN.equals(roleName);
    }

    private boolean isRequesterOwner() {
        Object roleAttribute = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes() != null
                ? ((org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.getRequestAttributes())
                .getRequest().getAttribute(BearerTokenInterceptor.REQUEST_ROLE_ATTRIBUTE)
                : null;

        String roleName = roleAttribute instanceof String ? ((String) roleAttribute).trim().toUpperCase() : null;
        return Role.OWNER.equals(roleName);
    }

    private Users getRequesterUser() {
        Object userIdAttribute = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes() != null
                ? ((org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.getRequestAttributes())
                .getRequest().getAttribute(BearerTokenInterceptor.REQUEST_USER_ID_ATTRIBUTE)
                : null;

        Long userId = userIdAttribute instanceof Number ? ((Number) userIdAttribute).longValue() : null;
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId).orElse(null);
    }

    private boolean isVisibleForWarehouses(ReceiptResponseDTO receipt, List<String> allowedWarehouseCodes) {
        if (receipt == null || allowedWarehouseCodes == null || allowedWarehouseCodes.isEmpty()) {
            return false;
        }

        String origin = receipt.getOrigin() == null ? "" : receipt.getOrigin().trim().toUpperCase(Locale.ROOT);
        String destiny = receipt.getDestiny() == null ? "" : receipt.getDestiny().trim().toUpperCase(Locale.ROOT);

        return allowedWarehouseCodes.contains(origin) || allowedWarehouseCodes.contains(destiny);
    }
}

