package com.example.scaffold.config;

import com.example.scaffold.domain.context.Module;
import com.example.scaffold.domain.context.Status;
import com.example.scaffold.domain.Audits.Keys;
import com.example.scaffold.domain.auths.Permissions;
import com.example.scaffold.domain.auths.Role;
import com.example.scaffold.domain.auths.RolePermissions;
import com.example.scaffold.domain.documents.DocumentsEnum;
import com.example.scaffold.domain.inventory.Article;
import com.example.scaffold.domain.inventory.Warehouse;
import com.example.scaffold.dto.auth.UserDTO;
import com.example.scaffold.repository.ArticleRepository;
import com.example.scaffold.repository.KeyRepository;
import com.example.scaffold.repository.ModuleRepository;
import com.example.scaffold.repository.PermissionRepository;
import com.example.scaffold.repository.RoleRepository;
import com.example.scaffold.repository.RolePermissionsRepository;
import com.example.scaffold.repository.StatusRepository;
import com.example.scaffold.repository.WarehouseRepository;
import com.example.scaffold.service.auths.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Configuration
public class SQLiteSeedDataConfig {
    private static final String WRITE_PERMISSION_CODE = "WRITE";
    private static final String READ_PERMISSION_CODE = "READ";
    private static final String USERS_MODULE_NAME = "Users";
    private static final String RECEIPTS_MODULE_NAME = "Receipts";
    private static final String ARTICLES_MODULE_NAME = "Articles";
    private static final String REPAIRS_MODULE_NAME = "Repairs";
    private static final String WAREHOUSES_MODULE_NAME = "Warehouses";
    private static final String USERS_MODULE_MAIN_ID = "MOD_USERS";
    private static final String RECEIPTS_MODULE_MAIN_ID = "MOD_RECEIPTS";
    private static final String ARTICLES_MODULE_MAIN_ID = "MOD_ARTICLES";
    private static final String REPAIRS_MODULE_MAIN_ID = "MOD_REPAIRS";
    private static final String WAREHOUSES_MODULE_MAIN_ID = "MOD_WAREHOUSES";
    @Bean
    @Profile("local")
    public CommandLineRunner seedAdminUser(UserService userService,
                                           RoleRepository roleRepository,
                                           ModuleRepository moduleRepository,
                                           PermissionRepository permissionRepository,
                                           RolePermissionsRepository rolePermissionsRepository,
                                           StatusRepository statusRepository,
                                           KeyRepository keyRepository,
                                            ArticleRepository articleRepository,
                                            WarehouseRepository warehouseRepository) {
        return args -> {
            ensureRole(roleRepository, Role.USER, "Default user role");
            ensureRole(roleRepository, Role.ADMIN, "Administration role");
            ensureRole(roleRepository, Role.OWNER, "System owner role");

            ensureUser(userService, "admin", "admin@local", "admin123", Role.ADMIN);
            ensureUser(userService, "owner", "owner@local", "owner123", Role.OWNER);
            ensureUser(userService, "user1", "user1@local", "user123", Role.USER);

            Module usersModule = ensureModule(moduleRepository, USERS_MODULE_NAME, USERS_MODULE_MAIN_ID);
            Module receiptsModule = ensureModule(moduleRepository, RECEIPTS_MODULE_NAME, RECEIPTS_MODULE_MAIN_ID);
            Module articlesModule = ensureModule(moduleRepository, ARTICLES_MODULE_NAME, ARTICLES_MODULE_MAIN_ID);
            Module repairsModule = ensureModule(moduleRepository, REPAIRS_MODULE_NAME, REPAIRS_MODULE_MAIN_ID);
            Module warehousesModule = ensureModule(moduleRepository, WAREHOUSES_MODULE_NAME, WAREHOUSES_MODULE_MAIN_ID);
            Permissions writePermission = ensurePermission(permissionRepository, WRITE_PERMISSION_CODE, "Write");
            Permissions readPermission = ensurePermission(permissionRepository, READ_PERMISSION_CODE, "Read");

            ensureRolePermission(roleRepository, rolePermissionsRepository, Role.USER, usersModule, writePermission, false);
            ensureRolePermission(roleRepository, rolePermissionsRepository, Role.USER, usersModule, readPermission, false);
            ensureRolePermission(roleRepository, rolePermissionsRepository, Role.ADMIN, usersModule, writePermission, true);
            ensureRolePermission(roleRepository, rolePermissionsRepository, Role.ADMIN, usersModule, readPermission, true);
            ensureRolePermission(roleRepository, rolePermissionsRepository, Role.OWNER, usersModule, writePermission, true);
            ensureRolePermission(roleRepository, rolePermissionsRepository, Role.OWNER, usersModule, readPermission, true);

            ensureRolePermission(roleRepository, rolePermissionsRepository, Role.USER, receiptsModule, writePermission, false);
            ensureRolePermission(roleRepository, rolePermissionsRepository, Role.USER, receiptsModule, readPermission, true);
            ensureRolePermission(roleRepository, rolePermissionsRepository, Role.ADMIN, receiptsModule, writePermission, true);
            ensureRolePermission(roleRepository, rolePermissionsRepository, Role.ADMIN, receiptsModule, readPermission, true);
            ensureRolePermission(roleRepository, rolePermissionsRepository, Role.OWNER, receiptsModule, writePermission, false);
            ensureRolePermission(roleRepository, rolePermissionsRepository, Role.OWNER, receiptsModule, readPermission, true);

            ensureRolePermission(roleRepository, rolePermissionsRepository, Role.USER, articlesModule, writePermission, false);
            ensureRolePermission(roleRepository, rolePermissionsRepository, Role.USER, articlesModule, readPermission, false);
            ensureRolePermission(roleRepository, rolePermissionsRepository, Role.ADMIN, articlesModule, writePermission, true);
            ensureRolePermission(roleRepository, rolePermissionsRepository, Role.ADMIN, articlesModule, readPermission, true);
            ensureRolePermission(roleRepository, rolePermissionsRepository, Role.OWNER, articlesModule, writePermission, true);
            ensureRolePermission(roleRepository, rolePermissionsRepository, Role.OWNER, articlesModule, readPermission, true);

            // Repairs: all roles can do ABM (USER restricted to their warehouse by policy)
            ensureRolePermission(roleRepository, rolePermissionsRepository, Role.USER, repairsModule, writePermission, true);
            ensureRolePermission(roleRepository, rolePermissionsRepository, Role.USER, repairsModule, readPermission, true);
            ensureRolePermission(roleRepository, rolePermissionsRepository, Role.ADMIN, repairsModule, writePermission, true);
            ensureRolePermission(roleRepository, rolePermissionsRepository, Role.ADMIN, repairsModule, readPermission, true);
            ensureRolePermission(roleRepository, rolePermissionsRepository, Role.OWNER, repairsModule, writePermission, true);
            ensureRolePermission(roleRepository, rolePermissionsRepository, Role.OWNER, repairsModule, readPermission, true);

            // Warehouses policy
            // USER: read only own warehouses and destiny receipts from those warehouses (service-level filtering)
            // ADMIN: read only associated warehouses, cannot modify warehouses
            // OWNER: full CRUD
            ensureRolePermission(roleRepository, rolePermissionsRepository, Role.USER, warehousesModule, writePermission, false);
            ensureRolePermission(roleRepository, rolePermissionsRepository, Role.USER, warehousesModule, readPermission, true);
            ensureRolePermission(roleRepository, rolePermissionsRepository, Role.ADMIN, warehousesModule, writePermission, false);
            ensureRolePermission(roleRepository, rolePermissionsRepository, Role.ADMIN, warehousesModule, readPermission, true);
            ensureRolePermission(roleRepository, rolePermissionsRepository, Role.OWNER, warehousesModule, writePermission, true);
            ensureRolePermission(roleRepository, rolePermissionsRepository, Role.OWNER, warehousesModule, readPermission, true);

            // Status catalog – status 0 is system-internal for deletion records
            ensureStatus(statusRepository, 0, "deleted");
            ensureStatus(statusRepository, 10, "new");
            ensureStatus(statusRepository, 55, "on preparation");
            ensureStatus(statusRepository, 75, "ready to dispatched");
            ensureStatus(statusRepository, 95, "dispatched");
            ensureStatus(statusRepository, 110, "receipt");

            ensureKey(keyRepository, DocumentsEnum.RECEIPT, "RCP");
            ensureKey(keyRepository, DocumentsEnum.ARTICLE, "ART");
            ensureKey(keyRepository, DocumentsEnum.ORDER, "ORD");
            ensureKey(keyRepository, DocumentsEnum.REPAIR, "RPR");

            ensureWarehouse(warehouseRepository, "WH-001", "Berazategui");
            ensureWarehouse(warehouseRepository, "WH-002", "Ezpeleta");
            ensureWarehouse(warehouseRepository, "WH-003", "Bernal");

            Long userId = userService.findByEmailDb("user1@local").map(com.example.scaffold.domain.auths.Users::getId).orElse(null);
            Long wh1Id = warehouseRepository.findByCode("WH-001").map(com.example.scaffold.domain.inventory.Warehouse::getId).orElse(null);
            Long wh2Id = warehouseRepository.findByCode("WH-002").map(com.example.scaffold.domain.inventory.Warehouse::getId).orElse(null);

            Long adminId = userService.findByEmailDb("admin@local").map(com.example.scaffold.domain.auths.Users::getId).orElse(null);
            if (adminId != null && wh1Id != null && wh2Id != null) {
                userService.updateAllowedWarehouses(adminId, java.util.Arrays.asList(wh1Id, wh2Id));
            }

            if (userId != null && wh1Id != null) {
                userService.updateAllowedWarehouses(userId, java.util.Collections.singletonList(wh1Id));
            }

            ensureArticle(articleRepository, "ART-0001", "Mountain Tire", "SPARE", "Contoso", "35.00", "49.90");
            ensureArticle(articleRepository, "ART-0002", "Hydraulic Brake Kit", "SPARE", "Fabrikam", "58.00", "84.00");
            ensureArticle(articleRepository, "ART-0003", "Carbon Handlebar", "COMPONENT", "Northwind", "72.00", "115.00");
        };
    }

    private void ensureStatus(StatusRepository statusRepository, Integer code, String description) {
        Status existing = statusRepository.findByStatus(code).orElse(null);
        if (existing != null) {
            if (description != null && !description.equals(existing.getDescription())) {
                existing.setDescription(description);
                statusRepository.save(existing);
            }
            return;
        }

        Status status = new Status();
        status.setStatus(code);
        status.setDescription(description);
        statusRepository.save(status);
    }

    private void ensureWarehouse(WarehouseRepository warehouseRepository, String code, String name) {
        Warehouse existing = warehouseRepository.findByCode(code).orElse(null);
        if (existing != null) {
            existing.setName(name);
            existing.setActive(true);
            if (existing.getCreationDate() == null) {
                existing.setCreationDate(LocalDateTime.now());
            }
            existing.setEditDate(LocalDateTime.now());
            warehouseRepository.save(existing);
            return;
        }

        Warehouse warehouse = new Warehouse();
        warehouse.setCode(code);
        warehouse.setName(name);
        warehouse.setActive(true);
        warehouse.setCreationDate(LocalDateTime.now());
        warehouse.setEditDate(LocalDateTime.now());
        warehouseRepository.save(warehouse);
    }

    private void ensureKey(KeyRepository keyRepository, DocumentsEnum document, String prefix) {
        Keys existing = keyRepository.findByTargetDestiny(document.getTargetDestiny()).orElse(null);
        if (existing != null) {
            existing.setPrefix(prefix);
            if (existing.getIncrementaNumberKey() == null || existing.getIncrementaNumberKey() < 0 || existing.getIncrementaNumberKey() > 9999999) {
                existing.setIncrementaNumberKey(0);
            }
            if (existing.getIncrementalLetterKey() == null || existing.getIncrementalLetterKey().trim().length() != 4) {
                existing.setIncrementalLetterKey("AAAA");
            }
            keyRepository.save(existing);
            return;
        }

        Keys key = new Keys();
        key.setPrefix(prefix);
        key.setTargetDestiny(document.getTargetDestiny());
        key.setIncrementaNumberKey(0);
        key.setIncrementalLetterKey("AAAA");
        keyRepository.save(key);
    }

    private void ensureArticle(ArticleRepository articleRepository,
                               String sku,
                               String name,
                               String type,
                               String supplier,
                               String purchasePrice,
                               String salePrice) {
        Article existing = articleRepository.findBySku(sku).orElse(null);
        if (existing != null) {
            return;
        }

        Article article = new Article();
        article.setSku(sku);
        article.setName(name);
        article.setType(type);
        article.setSupplier(supplier);
        article.setPurchasePrice(new BigDecimal(purchasePrice));
        article.setSalePrice(new BigDecimal(salePrice));
        articleRepository.save(article);
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

    private Module ensureModule(ModuleRepository moduleRepository, String moduleName, String moduleMainId) {
        Module existing = moduleRepository.findByName(moduleName).orElse(null);
        if (existing != null) {
            if (moduleMainId != null && !moduleMainId.equals(existing.getMainId())) {
                existing.setMainId(moduleMainId);
                return moduleRepository.save(existing);
            }
            return existing;
        }

        Module module = new Module();
        module.setMainId(moduleMainId);
        module.setName(moduleName);
        module.setParentId(null);
        return moduleRepository.save(module);
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
        Role role = roleRepository.findByName(roleName).orElse(null);
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
