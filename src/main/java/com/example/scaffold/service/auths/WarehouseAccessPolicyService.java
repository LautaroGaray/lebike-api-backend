package com.example.scaffold.service.auths;

import com.example.scaffold.domain.auths.Role;
import com.example.scaffold.domain.auths.Users;
import com.example.scaffold.domain.inventory.Warehouse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class WarehouseAccessPolicyService {

    private static final String POINT_OF_SALE_PREFIX = "P-";

    public Optional<String> validateUserConfiguration(Users user) {
        if (user == null || user.getRole() == null || !StringUtils.hasText(user.getRole().getName())) {
            return Optional.of("User role is required to evaluate warehouse access");
        }

        String roleName = normalizeRole(user.getRole().getName());
        int allowedCount = safeWarehouses(user).size();

        if (Role.OWNER.equals(roleName)) {
            return Optional.empty();
        }

        if (Role.USER.equals(roleName) && allowedCount != 1) {
            return Optional.of("USER must have exactly one allowed warehouse");
        }

        return Optional.empty();
    }

    public Optional<String> validateReceiptWarehouseAccess(Users user, String origin, String destiny) {
        Optional<String> configError = validateUserConfiguration(user);
        if (configError.isPresent()) {
            return configError;
        }

        String roleName = normalizeRole(user.getRole().getName());
        if (Role.OWNER.equals(roleName)) {
            return Optional.empty();
        }

        String normalizedOrigin = normalizeCode(origin);
        String normalizedDestiny = normalizeCode(destiny);

        List<String> warehouseCodes = new ArrayList<>();
        if (isWarehouseCode(normalizedOrigin)) {
            warehouseCodes.add(normalizedOrigin);
        }
        if (isWarehouseCode(normalizedDestiny)) {
            warehouseCodes.add(normalizedDestiny);
        }

        if (Role.ADMIN.equals(roleName)) {
            List<Warehouse> allowed = safeWarehouses(user);
            // ADMIN with empty list can access all warehouses.
            if (allowed.isEmpty()) {
                return Optional.empty();
            }
            for (String code : warehouseCodes) {
                if (!containsWarehouseCode(allowed, code)) {
                    return Optional.of("ADMIN is restricted and cannot access warehouse: " + code);
                }
            }
            return Optional.empty();
        }

        if (Role.USER.equals(roleName)) {
            String onlyAllowedCode = normalizeCode(safeWarehouses(user).get(0).getCode());
            if (warehouseCodes.isEmpty()) {
                return Optional.of("USER operation must include at least one warehouse code");
            }
            for (String code : warehouseCodes) {
                if (!onlyAllowedCode.equals(code)) {
                    return Optional.of("USER can only access warehouse: " + onlyAllowedCode);
                }
            }
        }

        return Optional.empty();
    }

    public Optional<String> validateSingleWarehouseAccess(Users user, String warehouseCode) {
        Optional<String> configError = validateUserConfiguration(user);
        if (configError.isPresent()) {
            return configError;
        }

        if (!StringUtils.hasText(warehouseCode)) {
            return Optional.of("warehouseCode is required");
        }

        String roleName = normalizeRole(user.getRole().getName());
        if (Role.OWNER.equals(roleName)) {
            return Optional.empty();
        }

        if (Role.ADMIN.equals(roleName) && safeWarehouses(user).isEmpty()) {
            return Optional.empty();
        }

        String normalized = normalizeCode(warehouseCode);
        if (!containsWarehouseCode(safeWarehouses(user), normalized)) {
            return Optional.of("User cannot access warehouse: " + normalized);
        }

        return Optional.empty();
    }

    private List<Warehouse> safeWarehouses(Users user) {
        return user.getWarehousesAllowed() == null ? new ArrayList<>() : user.getWarehousesAllowed();
    }

    private boolean containsWarehouseCode(List<Warehouse> warehouses, String code) {
        for (Warehouse warehouse : warehouses) {
            if (warehouse == null) {
                continue;
            }
            if (normalizeCode(warehouse.getCode()).equals(code)) {
                return true;
            }
        }
        return false;
    }

    private boolean isWarehouseCode(String code) {
        return StringUtils.hasText(code) && !code.startsWith(POINT_OF_SALE_PREFIX);
    }

    private String normalizeRole(String roleName) {
        return roleName == null ? "" : roleName.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeCode(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }
}

