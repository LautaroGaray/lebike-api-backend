package com.example.scaffold.service;

import com.example.scaffold.domain.Module;
import com.example.scaffold.domain.RolePermissions;
import com.example.scaffold.domain.UserPermissions;
import com.example.scaffold.domain.Users;
import com.example.scaffold.dto.auth.UserDTO;
import com.example.scaffold.repository.ModuleRepository;
import com.example.scaffold.repository.RolePermissionsRepository;
import com.example.scaffold.repository.UserPermissionsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class ModuleService {

    private final ModuleRepository moduleRepository;
    private final RolePermissionsRepository rolePermissionsRepository;
    private final UserPermissionsRepository userPermissionsRepository;
    private final UserService userService;

    public ModuleService(ModuleRepository moduleRepository,
                         RolePermissionsRepository rolePermissionsRepository,
                         UserPermissionsRepository userPermissionsRepository,
                         UserService userService) {
        this.moduleRepository = moduleRepository;
        this.rolePermissionsRepository = rolePermissionsRepository;
        this.userPermissionsRepository = userPermissionsRepository;
        this.userService = userService;

    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Optional<List<Module>> loadModulesByUser(UserDTO userDTO) {
        if (userDTO == null || userDTO.getEmail() == null || userDTO.getEmail().trim().isEmpty()) {
            return Optional.empty();
        }

        Users user = userService.findByEmailDb(userDTO.getEmail()).orElse(null);

        if (Objects.isNull(user)) {
            return Optional.empty();
        }

        List<Module> moduleList = moduleRepository.findAll();
        if (moduleList.isEmpty()) {
            return Optional.of(new ArrayList<>());
        }

        Map<Long, Module> modulesById = new LinkedHashMap<>();
        for (Module module : moduleList) {
            module.setChildren(new ArrayList<>());
            module.setPermissions(new LinkedHashMap<>());
            modulesById.put(module.getId(), module);
        }

        List<RolePermissions> rolePermissions = rolePermissionsRepository.findByRoleId(user.getRole().getId());
        applyRolePermissions(rolePermissions, modulesById);

        List<UserPermissions> userPermissions = userPermissionsRepository.findByUserId(user.getId());
        applyUserOverrides(userPermissions, modulesById);

        List<Module> rootModules = buildModuleTree(moduleList, modulesById);
        return Optional.of(rootModules);
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Optional<Module> reloadModuleWithChildrenByUser(UserDTO userDTO, Long moduleId, String moduleMainId) {
        List<Module> roots = loadModulesByUser(userDTO).orElse(null);
        if (roots == null) {
            return Optional.empty();
        }

        Module selected = findModuleRecursive(roots, moduleId, moduleMainId);
        if (selected == null) {
            return Optional.empty();
        }

        return hasAnyEnabledPermissionInTree(selected) ? Optional.of(selected) : Optional.empty();
    }

    private void applyRolePermissions(List<RolePermissions> rolePermissions, Map<Long, Module> modulesById) {
        for (RolePermissions row : rolePermissions) {
            if (row.getModule() == null || row.getPermission() == null) {
                continue;
            }
            Module module = modulesById.get(row.getModule().getId());
            if (module == null) {
                continue;
            }
            String permissionKey = buildPermissionKey(row.getPermission());
            module.getPermissions().put(permissionKey, row.isEnabled());
        }
    }

    private void applyUserOverrides(List<UserPermissions> userPermissions, Map<Long, Module> modulesById) {
        for (UserPermissions row : userPermissions) {
            if (row.getModule() == null || row.getPermission() == null) {
                continue;
            }
            Module module = modulesById.get(row.getModule().getId());
            if (module == null) {
                continue;
            }
            String permissionKey = buildPermissionKey(row.getPermission());
            module.getPermissions().put(permissionKey, row.isEnabled());
        }
    }

    private List<Module> buildModuleTree(List<Module> modules, Map<Long, Module> modulesById) {
        List<Module> rootModules = new ArrayList<>();
        for (Module module : modules) {
            Long parentId = module.getParentId();
            if (parentId != null && modulesById.containsKey(parentId)) {
                modulesById.get(parentId).getChildren().add(module);
            } else {
                rootModules.add(module);
            }
        }
        return rootModules;
    }

    private String buildPermissionKey(com.example.scaffold.domain.Permissions permission) {
        if (permission.getCode() != null && !permission.getCode().trim().isEmpty()) {
            return permission.getCode().trim().toUpperCase();
        }
        if (permission.getName() != null && !permission.getName().trim().isEmpty()) {
            return permission.getName().trim().toUpperCase();
        }
        return "PERMISSION_" + permission.getId();
    }

    private Module findModuleRecursive(List<Module> modules, Long moduleId, String moduleMainId) {
        for (Module module : modules) {
            boolean matchesById = moduleId != null && moduleId.equals(module.getId());
            boolean matchesByMainId = moduleMainId != null && moduleMainId.equalsIgnoreCase(module.getMainId());
            if (matchesById || matchesByMainId) {
                return module;
            }

            Module childMatch = findModuleRecursive(module.getChildren(), moduleId, moduleMainId);
            if (childMatch != null) {
                return childMatch;
            }
        }
        return null;
    }

    private boolean hasAnyEnabledPermissionInTree(Module module) {
        boolean selfHasPermission = module.getPermissions().values().stream().anyMatch(Boolean.TRUE::equals);
        if (selfHasPermission) {
            return true;
        }
        for (Module child : module.getChildren()) {
            if (hasAnyEnabledPermissionInTree(child)) {
                return true;
            }
        }
        return false;
    }

}
