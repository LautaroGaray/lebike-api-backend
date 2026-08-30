package com.example.scaffold.service.auths;

import com.example.scaffold.domain.auths.Role;
import com.example.scaffold.domain.auths.Users;
import com.example.scaffold.domain.inventory.Warehouse;
import com.example.scaffold.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class DocumentWarehouseScopeService {

    private final UserRepository userRepository;

    public DocumentWarehouseScopeService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Users getRequesterOrThrow(Long requesterUserId) {
        if (requesterUserId == null) {
            throw new IllegalArgumentException("Requester user is not authenticated");
        }
        return userRepository.findById(requesterUserId)
                .orElseThrow(() -> new IllegalArgumentException("Requester user not found"));
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public boolean canAccessDocument(Users requester, String origin, String destiny) {
        String role = normalizeRoleName(requester != null && requester.getRole() != null ? requester.getRole().getName() : null);
        if (Role.OWNER.equals(role)) {
            return true;
        }

        List<String> allowedCodes = toAllowedWarehouseCodes(requester);
        if (allowedCodes.isEmpty()) {
            return false;
        }

        String normalizedOrigin = normalizeWarehouseCode(origin);
        String normalizedDestiny = normalizeWarehouseCode(destiny);

        if (Role.ADMIN.equals(role)) {
            return allowedCodes.contains(normalizedOrigin) && allowedCodes.contains(normalizedDestiny);
        }

        if (Role.USER.equals(role)) {
            if (allowedCodes.size() != 1) {
                return false;
            }
            return allowedCodes.contains(normalizedDestiny);
        }

        return false;
    }

    public void assertCanAccessDocument(Users requester, String origin, String destiny) {
        if (!canAccessDocument(requester, origin, destiny)) {
            throw new IllegalArgumentException("Receipt access denied for requester scope");
        }
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public List<String> toAllowedWarehouseCodes(Users requester) {
        if (requester == null || requester.getWarehousesAllowed() == null) {
            return Collections.emptyList();
        }
        List<String> codes = new ArrayList<>();
        for (Warehouse warehouse : requester.getWarehousesAllowed()) {
            if (warehouse == null || !StringUtils.hasText(warehouse.getCode())) {
                continue;
            }
            codes.add(normalizeWarehouseCode(warehouse.getCode()));
        }
        return codes;
    }

    public boolean isOwner(Users requester) {
        String role = normalizeRoleName(requester != null && requester.getRole() != null ? requester.getRole().getName() : null);
        return Role.OWNER.equals(role);
    }

    private String normalizeRoleName(String roleName) {
        return StringUtils.hasText(roleName) ? roleName.trim().toUpperCase(Locale.ROOT) : "";
    }

    private String normalizeWarehouseCode(String code) {
        if (!StringUtils.hasText(code)) {
            return "";
        }
        return code.trim().replace('_', '-').toUpperCase(Locale.ROOT);
    }

    public boolean canAccessWarehouse(Users requester, String warehouseCode) {
        String role = normalizeRoleName(requester != null && requester.getRole() != null ? requester.getRole().getName() : null);
        if (Role.OWNER.equals(role)) {
            return true;
        }

        List<String> allowedCodes = toAllowedWarehouseCodes(requester);
        if (allowedCodes.isEmpty()) {
            return false;
        }

        String normalizedCode = normalizeWarehouseCode(warehouseCode);
        if (Role.USER.equals(role) && allowedCodes.size() != 1) {
            return false;
        }
        return allowedCodes.contains(normalizedCode);
    }

    public void assertCanAccessWarehouse(Users requester, String warehouseCode, String resourceName) {
        if (!canAccessWarehouse(requester, warehouseCode)) {
            throw new IllegalArgumentException(resourceName + " access denied for requester scope");
        }
    }

    public boolean canViewHistory(Users requester) {
        String role = normalizeRoleName(requester != null && requester.getRole() != null ? requester.getRole().getName() : null);
        return !Role.USER.equals(role);
    }

    public void assertCanViewHistory(Users requester, String resourceName) {
        if (!canViewHistory(requester)) {
            throw new IllegalArgumentException(resourceName + " history is not allowed for USER role");
        }
    }
}
