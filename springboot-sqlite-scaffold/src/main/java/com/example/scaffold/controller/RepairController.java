package com.example.scaffold.controller;

import com.example.scaffold.domain.auths.Permissions;
import com.example.scaffold.domain.auths.Role;
import com.example.scaffold.domain.auths.RolePermissions;
import com.example.scaffold.domain.auths.Users;
import com.example.scaffold.domain.context.Module;
import com.example.scaffold.domain.inventory.Repair;
import com.example.scaffold.domain.inventory.Warehouse;
import com.example.scaffold.dto.ResponseData;
import com.example.scaffold.dto.inventory.RepairAuditResponseDTO;
import com.example.scaffold.dto.inventory.RepairRequestDTO;
import com.example.scaffold.dto.inventory.RepairResponseDTO;
import com.example.scaffold.repository.ModuleRepository;
import com.example.scaffold.repository.PermissionRepository;
import com.example.scaffold.repository.RepairRepository;
import com.example.scaffold.repository.RolePermissionsRepository;
import com.example.scaffold.repository.UserRepository;
import com.example.scaffold.repository.WarehouseRepository;
import com.example.scaffold.security.BearerTokenInterceptor;
import com.example.scaffold.service.auths.WarehouseAccessPolicyService;
import com.example.scaffold.service.inventory.RepairService;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/repairs")
public class RepairController {

    private static final String REPAIRS_MODULE = "Repairs";
    private static final String WRITE_PERMISSION = "WRITE";
    private static final String READ_PERMISSION = "READ";

    private final RepairService repairService;
    private final RepairRepository repairRepository;
    private final UserRepository userRepository;
    private final WarehouseRepository warehouseRepository;
    private final ModuleRepository moduleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionsRepository rolePermissionsRepository;
    private final WarehouseAccessPolicyService warehouseAccessPolicyService;

    public RepairController(RepairService repairService,
                            RepairRepository repairRepository,
                            UserRepository userRepository,
                            WarehouseRepository warehouseRepository,
                            ModuleRepository moduleRepository,
                            PermissionRepository permissionRepository,
                            RolePermissionsRepository rolePermissionsRepository,
                            WarehouseAccessPolicyService warehouseAccessPolicyService) {
        this.repairService = repairService;
        this.repairRepository = repairRepository;
        this.userRepository = userRepository;
        this.warehouseRepository = warehouseRepository;
        this.moduleRepository = moduleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionsRepository = rolePermissionsRepository;
        this.warehouseAccessPolicyService = warehouseAccessPolicyService;
    }

    @PostMapping("/register")
    public ResponseEntity<ResponseData> register(@RequestBody RepairRequestDTO request) {
        if (!hasRequesterPermission(WRITE_PERMISSION)) {
            return ResponseEntity.status(403).body(new ResponseData(null, false, "Insufficient permissions to create repairs"));
        }

        Users requesterUser = getRequesterUser();
        if (requesterUser == null) {
            return ResponseEntity.status(403).body(new ResponseData(null, false, "Unable to determine requester user"));
        }

        if (request == null || request.getWarehouseId() == null) {
            return ResponseEntity.badRequest().body(new ResponseData(null, false, "warehouseId is required"));
        }

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId()).orElse(null);
        if (warehouse == null || !warehouse.isActive()) {
            return ResponseEntity.status(404).body(new ResponseData(null, false, "Warehouse not found or inactive"));
        }

        String warehouseAccessError = warehouseAccessPolicyService
                .validateSingleWarehouseAccess(requesterUser, warehouse.getCode())
                .orElse(null);
        if (warehouseAccessError != null) {
            return ResponseEntity.status(403).body(new ResponseData(null, false, warehouseAccessError));
        }

        try {
            RepairResponseDTO created = repairService.create(request, requesterUser.getId());
            return ResponseEntity.ok(new ResponseData(created, true, "Repair created successfully"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ResponseData(null, false, ex.getMessage()));
        }
    }

    @PutMapping("/edit/{repairId}")
    public ResponseEntity<ResponseData> edit(@PathVariable Long repairId,
                                             @RequestBody RepairRequestDTO request) {
        if (!hasRequesterPermission(WRITE_PERMISSION)) {
            return ResponseEntity.status(403).body(new ResponseData(null, false, "Insufficient permissions to update repairs"));
        }

        if (repairId == null) {
            return ResponseEntity.badRequest().body(new ResponseData(null, false, "repairId is required"));
        }

        Repair existing = repairRepository.findById(repairId).orElse(null);
        if (existing == null) {
            return ResponseEntity.status(404).body(new ResponseData(null, false, "Repair not found"));
        }

        Users requesterUser = getRequesterUser();
        if (requesterUser == null) {
            return ResponseEntity.status(403).body(new ResponseData(null, false, "Unable to determine requester user"));
        }

        // Check warehouse access against current repair's warehouse
        String warehouseCode = existing.getWarehouse() != null ? existing.getWarehouse().getCode() : null;
        String accessError = warehouseAccessPolicyService
                .validateSingleWarehouseAccess(requesterUser, warehouseCode)
                .orElse(null);
        if (accessError != null) {
            return ResponseEntity.status(403).body(new ResponseData(null, false, accessError));
        }

        try {
            RepairResponseDTO updated = repairService.update(repairId, request, requesterUser.getId());
            return ResponseEntity.ok(new ResponseData(updated, true, "Repair updated successfully"));
        } catch (IllegalArgumentException ex) {
            String msg = ex.getMessage();
            if ("Repair not found".equals(msg)) {
                return ResponseEntity.status(404).body(new ResponseData(null, false, msg));
            }
            return ResponseEntity.badRequest().body(new ResponseData(null, false, msg));
        }
    }

    @DeleteMapping("/delete/{repairId}")
    public ResponseEntity<ResponseData> delete(@PathVariable Long repairId) {
        if (!hasRequesterPermission(WRITE_PERMISSION)) {
            return ResponseEntity.status(403).body(new ResponseData(null, false, "Insufficient permissions to delete repairs"));
        }

        if (repairId == null) {
            return ResponseEntity.badRequest().body(new ResponseData(null, false, "repairId is required"));
        }

        Repair existing = repairRepository.findById(repairId).orElse(null);
        if (existing == null) {
            return ResponseEntity.status(404).body(new ResponseData(null, false, "Repair not found"));
        }

        Users requesterUser = getRequesterUser();
        if (requesterUser == null) {
            return ResponseEntity.status(403).body(new ResponseData(null, false, "Unable to determine requester user"));
        }

        String warehouseCode = existing.getWarehouse() != null ? existing.getWarehouse().getCode() : null;
        String accessError = warehouseAccessPolicyService
                .validateSingleWarehouseAccess(requesterUser, warehouseCode)
                .orElse(null);
        if (accessError != null) {
            return ResponseEntity.status(403).body(new ResponseData(null, false, accessError));
        }

        try {
            repairService.delete(repairId, requesterUser.getId());
            return ResponseEntity.ok(new ResponseData(null, true, "Repair deleted successfully"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(new ResponseData(null, false, ex.getMessage()));
        }
    }

    @GetMapping("/findAll")
    public ResponseEntity<ResponseData> findAll() {
        if (!hasRequesterPermission(READ_PERMISSION)) {
            return ResponseEntity.status(403).body(new ResponseData(null, false, "Insufficient permissions to read repairs"));
        }

        Users requesterUser = getRequesterUser();
        if (requesterUser == null) {
            return ResponseEntity.status(403).body(new ResponseData(null, false, "Unable to determine requester user"));
        }

        String roleUpper = requesterUser.getRole() != null
                ? requesterUser.getRole().getName().trim().toUpperCase(Locale.ROOT) : "";

        List<RepairResponseDTO> repairs = repairService.findAll();

        if (!Role.OWNER.equals(roleUpper)) {
            List<String> allowedCodes = requesterUser.getWarehousesAllowed() == null
                    ? Collections.emptyList()
                    : requesterUser.getWarehousesAllowed().stream()
                    .filter(Objects::nonNull)
                    .map(w -> w.getCode() == null ? null : w.getCode().trim().toUpperCase(Locale.ROOT))
                    .filter(StringUtils::hasText)
                    .distinct()
                    .collect(Collectors.toList());

            if (Role.ADMIN.equals(roleUpper) && allowedCodes.isEmpty()) {
                return ResponseEntity.ok(new ResponseData(repairs, true, "Repairs retrieved successfully"));
            }

            repairs = repairs.stream()
                    .filter(r -> r.getWarehouseCode() != null
                            && allowedCodes.contains(r.getWarehouseCode().trim().toUpperCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }

        return ResponseEntity.ok(new ResponseData(repairs, true, "Repairs retrieved successfully"));
    }

    @GetMapping("/history/{repairId}")
    public ResponseEntity<ResponseData> history(@PathVariable Long repairId) {
        if (!isRequesterOwner()) {
            return ResponseEntity.status(403).body(new ResponseData(null, false, "Only OWNER can read repair history"));
        }
        if (!hasRequesterPermission(READ_PERMISSION)) {
            return ResponseEntity.status(403).body(new ResponseData(null, false, "Insufficient permissions to read repair history"));
        }
        if (repairId == null) {
            return ResponseEntity.badRequest().body(new ResponseData(null, false, "repairId is required"));
        }

        List<RepairAuditResponseDTO> history = repairService.findHistory(repairId);
        return ResponseEntity.ok(new ResponseData(history, true, "Repair history retrieved successfully"));
    }

    // ---- helper methods ----

    private boolean hasRequesterPermission(String permissionCode) {
        Long requesterRoleId = getRequesterRoleId();
        if (requesterRoleId == null || !StringUtils.hasText(permissionCode)) return false;

        Module module = moduleRepository.findByName(REPAIRS_MODULE).orElse(null);
        Permissions permission = permissionRepository.findByCode(permissionCode).orElse(null);
        if (module == null || permission == null) return false;

        RolePermissions rp = rolePermissionsRepository
                .findByRoleIdAndModuleIdAndPermissionId(requesterRoleId, module.getId(), permission.getId())
                .orElse(null);
        return rp != null && rp.isEnabled();
    }

    private Long getRequesterRoleId() {
        Object attr = getRequestAttr(BearerTokenInterceptor.REQUEST_ROLE_ID_ATTRIBUTE);
        return attr instanceof Number ? ((Number) attr).longValue() : null;
    }

    private boolean isRequesterOwner() {
        Object attr = getRequestAttr(BearerTokenInterceptor.REQUEST_ROLE_ATTRIBUTE);
        String roleName = attr instanceof String ? ((String) attr).trim().toUpperCase() : null;
        return Role.OWNER.equals(roleName);
    }

    private Users getRequesterUser() {
        Object attr = getRequestAttr(BearerTokenInterceptor.REQUEST_USER_ID_ATTRIBUTE);
        Long userId = attr instanceof Number ? ((Number) attr).longValue() : null;
        if (userId == null) return null;
        return userRepository.findById(userId).orElse(null);
    }

    private Object getRequestAttr(String attrName) {
        ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return sra != null ? sra.getRequest().getAttribute(attrName) : null;
    }
}

