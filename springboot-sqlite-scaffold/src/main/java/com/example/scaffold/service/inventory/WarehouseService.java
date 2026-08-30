 package com.example.scaffold.service.inventory;

import com.example.scaffold.domain.auths.Role;
import com.example.scaffold.domain.auths.Users;
import com.example.scaffold.domain.inventory.Warehouse;
import com.example.scaffold.dto.inventory.WarehouseRequestDTO;
import com.example.scaffold.dto.inventory.WarehouseResponseDTO;
import com.example.scaffold.repository.UserRepository;
import com.example.scaffold.repository.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final UserRepository userRepository;

    public WarehouseService(WarehouseRepository warehouseRepository,
                            UserRepository userRepository) {
        this.warehouseRepository = warehouseRepository;
        this.userRepository = userRepository;
    }

    public WarehouseResponseDTO create(WarehouseRequestDTO request, Long requesterId) {
        Users requester = findRequester(requesterId);
        ensureOwner(requester);

        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }

        String code = normalizeWarehouseCode(request.getCode());
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("Warehouse code is required");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("Warehouse name is required");
        }
        if (warehouseRepository.findByCode(code).isPresent()) {
            throw new IllegalArgumentException("Warehouse code already exists: " + code);
        }

        Warehouse warehouse = new Warehouse();
        warehouse.setCode(code);
        warehouse.setName(request.getName().trim());
        warehouse.setActive(request.getActive() == null || request.getActive());
        warehouse.setCreationDate(LocalDateTime.now());
        warehouse.setEditDate(LocalDateTime.now());

        return toWarehouseDto(warehouseRepository.save(warehouse));
    }

    public WarehouseResponseDTO update(Long warehouseId, WarehouseRequestDTO request, Long requesterId) {
        Users requester = findRequester(requesterId);
        ensureOwner(requester);

        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }

        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found"));

        String code = normalizeWarehouseCode(request.getCode());
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("Warehouse code is required");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("Warehouse name is required");
        }

        Warehouse sameCode = warehouseRepository.findByCode(code).orElse(null);
        if (sameCode != null && !sameCode.getId().equals(warehouseId)) {
            throw new IllegalArgumentException("Warehouse code already exists: " + code);
        }

        warehouse.setCode(code);
        warehouse.setName(request.getName().trim());
        if (request.getActive() != null) {
            warehouse.setActive(request.getActive());
        }
        warehouse.setEditDate(LocalDateTime.now());

        return toWarehouseDto(warehouseRepository.save(warehouse));
    }

    public void delete(Long warehouseId, Long requesterId) {
        Users requester = findRequester(requesterId);
        ensureOwner(requester);

        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found"));
        warehouseRepository.deleteById(warehouse.getId());
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public List<WarehouseResponseDTO> findVisible(Long requesterId) {
        Users requester = findRequester(requesterId);
        String role = normalizeRoleName(requester.getRole() != null ? requester.getRole().getName() : null);

        if (Role.OWNER.equals(role)) {
            return toWarehouseDtoList(warehouseRepository.findAll());
        }

        List<Warehouse> allowed = safeWarehouses(requester);
        return toWarehouseDtoList(allowed);
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public WarehouseResponseDTO findVisibleById(Long warehouseId, Long requesterId) {
        Users requester = findRequester(requesterId);
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found"));

        String role = normalizeRoleName(requester.getRole() != null ? requester.getRole().getName() : null);
        if (!Role.OWNER.equals(role) && !containsWarehouseId(safeWarehouses(requester), warehouseId)) {
            throw new IllegalArgumentException("User cannot access this warehouse");
        }

        return toWarehouseDto(warehouse);
    }

    private Users findRequester(Long requesterId) {
        if (requesterId == null) {
            throw new IllegalArgumentException("Requester user is not authenticated");
        }
        return userRepository.findById(requesterId)
                .orElseThrow(() -> new IllegalArgumentException("Requester user not found"));
    }

    private void ensureOwner(Users requester) {
        String role = normalizeRoleName(requester != null && requester.getRole() != null ? requester.getRole().getName() : null);
        if (!Role.OWNER.equals(role)) {
            throw new IllegalArgumentException("Only OWNER can modify warehouses");
        }
    }

    private List<Warehouse> safeWarehouses(Users user) {
        return user.getWarehousesAllowed() == null ? Collections.emptyList() : user.getWarehousesAllowed();
    }

    private String normalizeWarehouseCode(String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        String normalized = code.trim();
        if (normalized.startsWith("\"") && normalized.endsWith("\"") && normalized.length() >= 2) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        return normalized.replace('_', '-').toUpperCase(Locale.ROOT);
    }

    private String normalizeRoleName(String roleName) {
        return StringUtils.hasText(roleName) ? roleName.trim().toUpperCase(Locale.ROOT) : "";
    }

    private boolean containsWarehouseId(List<Warehouse> warehouses, Long warehouseId) {
        if (warehouseId == null) {
            return false;
        }
        for (Warehouse warehouse : warehouses) {
            if (warehouse != null && warehouseId.equals(warehouse.getId())) {
                return true;
            }
        }
        return false;
    }

    private List<WarehouseResponseDTO> toWarehouseDtoList(List<Warehouse> warehouses) {
        List<WarehouseResponseDTO> response = new ArrayList<>();
        for (Warehouse warehouse : warehouses) {
            if (warehouse == null) {
                continue;
            }
            response.add(toWarehouseDto(warehouse));
        }
        return response;
    }

    private WarehouseResponseDTO toWarehouseDto(Warehouse warehouse) {
        WarehouseResponseDTO dto = new WarehouseResponseDTO();
        dto.setId(warehouse.getId());
        dto.setCode(warehouse.getCode());
        dto.setName(warehouse.getName());
        dto.setActive(warehouse.isActive());
        dto.setCreationDate(warehouse.getCreationDate());
        dto.setEditDate(warehouse.getEditDate());
        return dto;
    }
}
