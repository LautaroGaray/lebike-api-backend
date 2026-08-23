package com.example.scaffold.config;

import com.example.scaffold.domain.Module;
import com.example.scaffold.domain.Permissions;
import com.example.scaffold.domain.Role;
import com.example.scaffold.domain.RolePermissions;
import com.example.scaffold.dto.auth.UserDTO;
import com.example.scaffold.repository.ModuleRepository;
import com.example.scaffold.repository.PermissionRepository;
import com.example.scaffold.repository.RoleRepository;
import com.example.scaffold.repository.RolePermissionsRepository;
import com.example.scaffold.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class SQLiteSeedDataConfig {
    private static final String WRITE_PERMISSION_CODE = "WRITE";


    @Bean
    @Profile("local")
    public CommandLineRunner seedAdminUser(UserService userService,
                                           RoleRepository roleRepository,
                                           ModuleRepository moduleRepository,
                                           PermissionRepository permissionRepository,
                                           RolePermissionsRepository rolePermissionsRepository) {
        return args -> {
            ensureRole(roleRepository, Role.USER, "Default user role");
            ensureRole(roleRepository, Role.ADMIN, "Administration role");
            ensureRole(roleRepository, Role.OWNER, "System owner role");

            ensureUser(userService, "admin", "admin@local", "admin123", Role.ADMIN);
            ensureUser(userService, "owner", "owner@local", "owner123", Role.OWNER);

            Module usersModule = ensureUsersModule(moduleRepository);
            Permissions writePermission = ensurePermission(permissionRepository, WRITE_PERMISSION_CODE, "Write");

            ensureRolePermission(roleRepository, rolePermissionsRepository, Role.USER, usersModule, writePermission, false);
            ensureRolePermission(roleRepository, rolePermissionsRepository, Role.ADMIN, usersModule, writePermission, true);
            ensureRolePermission(roleRepository, rolePermissionsRepository, Role.OWNER, usersModule, writePermission, true);
        };
    }

    private void ensureRole(RoleRepository roleRepository, String roleName, String description) {
        if (!roleRepository.findByName(roleName).isPresent()) {
            roleRepository.save(new Role(roleName, description));
        }
    }

    private void ensureUser(UserService userService, String nickName, String email, String password, String roleName) {
        if (userService.findByEmail(email).isPresent()) {
            return;
        }

        UserDTO user = new UserDTO();
        user.setNickName(nickName);
        user.setEmail(email);
        user.setPassword(password);
        user.setRoleName(roleName);
        try {
            userService.create(user);
        } catch (IllegalArgumentException e) {
            // Usuario ya existe, continuar
        }
    }

    private Module ensureUsersModule(ModuleRepository moduleRepository) {
        Module existing = moduleRepository.findByName("Users").orElse(null);
        if (existing != null) {
            return existing;
        }

        Module usersModule = new Module();
        usersModule.setName("Users");
        usersModule.setParentId(null);
        return moduleRepository.save(usersModule);
    }

    private Permissions ensurePermission(PermissionRepository permissionRepository, String code, String name) {
        return permissionRepository.findByCode(code)
                .orElseGet(() -> permissionRepository.save(new Permissions(code, name)));
    }

    private void ensureRolePermission(RoleRepository roleRepository,
                                      RolePermissionsRepository rolePermissionsRepository,
                                      String roleName,
                                      Module module,
                                      Permissions permission,
                                      boolean enabled) {
        com.example.scaffold.domain.Role role = roleRepository.findByName(roleName).orElse(null);
        if (role == null) {
            return;
        }

        RolePermissions rolePermission = rolePermissionsRepository
                .findByRoleIdAndModuleIdAndPermissionId(role.getId(), module.getId(), permission.getId())
                .orElse(null);

        if (rolePermission == null) {
            rolePermission = new RolePermissions();
            rolePermission.setRole(role);
            rolePermission.setModule(module);
            rolePermission.setPermission(permission);
        }

        rolePermission.setEnabled(enabled);
        rolePermissionsRepository.save(rolePermission);
    }
}

