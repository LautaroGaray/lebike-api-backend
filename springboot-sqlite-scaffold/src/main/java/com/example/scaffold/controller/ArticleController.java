package com.example.scaffold.controller;

import com.example.scaffold.domain.auths.Permissions;
import com.example.scaffold.domain.auths.Role;
import com.example.scaffold.domain.auths.RolePermissions;
import com.example.scaffold.domain.context.Module;
import com.example.scaffold.dto.ResponseData;
import com.example.scaffold.dto.inventory.ArticleRequestDTO;
import com.example.scaffold.dto.inventory.ArticleResponseDTO;
import com.example.scaffold.repository.ModuleRepository;
import com.example.scaffold.repository.PermissionRepository;
import com.example.scaffold.repository.RolePermissionsRepository;
import com.example.scaffold.security.BearerTokenInterceptor;
import com.example.scaffold.service.inventory.ArticleService;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/articles")
public class ArticleController {

    private static final String ARTICLES_MODULE = "Articles";
    private static final String WRITE_PERMISSION = "WRITE";

    private final ArticleService articleService;
    private final ModuleRepository moduleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionsRepository rolePermissionsRepository;

    public ArticleController(ArticleService articleService,
                             ModuleRepository moduleRepository,
                             PermissionRepository permissionRepository,
                             RolePermissionsRepository rolePermissionsRepository) {
        this.articleService = articleService;
        this.moduleRepository = moduleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionsRepository = rolePermissionsRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<ResponseData> register(@RequestBody ArticleRequestDTO request) {
        if (!isRequesterAdminOrOwner()) {
            return ResponseEntity.status(403).body(new ResponseData(null, false, "Only ADMIN or OWNER can create articles"));
        }
        if (!hasRequesterPermission(WRITE_PERMISSION)) {
            return ResponseEntity.status(403).body(new ResponseData(null, false, "Insufficient permissions to create articles"));
        }

        try {
            ArticleResponseDTO created = articleService.create(request);
            return ResponseEntity.ok(new ResponseData(created, true, "Article created successfully"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ResponseData(null, false, ex.getMessage()));
        }
    }

    @PutMapping("/edit/{articleId}")
    public ResponseEntity<ResponseData> edit(@PathVariable Long articleId,
                                             @RequestBody ArticleRequestDTO request) {
        if (!isRequesterAdminOrOwner()) {
            return ResponseEntity.status(403).body(new ResponseData(null, false, "Only ADMIN or OWNER can modify articles"));
        }
        if (!hasRequesterPermission(WRITE_PERMISSION)) {
            return ResponseEntity.status(403).body(new ResponseData(null, false, "Insufficient permissions to update articles"));
        }

        try {
            ArticleResponseDTO updated = articleService.update(articleId, request);
            return ResponseEntity.ok(new ResponseData(updated, true, "Article updated successfully"));
        } catch (IllegalArgumentException ex) {
            String message = ex.getMessage();
            if ("Article not found".equals(message)) {
                return ResponseEntity.status(404).body(new ResponseData(null, false, message));
            }
            return ResponseEntity.badRequest().body(new ResponseData(null, false, message));
        }
    }

    @DeleteMapping("/delete/{articleId}")
    public ResponseEntity<ResponseData> delete(@PathVariable Long articleId) {
        if (!isRequesterAdminOrOwner()) {
            return ResponseEntity.status(403).body(new ResponseData(null, false, "Only ADMIN or OWNER can delete articles"));
        }
        if (!hasRequesterPermission(WRITE_PERMISSION)) {
            return ResponseEntity.status(403).body(new ResponseData(null, false, "Insufficient permissions to delete articles"));
        }

        try {
            articleService.delete(articleId);
            return ResponseEntity.ok(new ResponseData(null, true, "Article deleted successfully"));
        } catch (IllegalArgumentException ex) {
            String message = ex.getMessage();
            if ("Article not found".equals(message)) {
                return ResponseEntity.status(404).body(new ResponseData(null, false, message));
            }
            return ResponseEntity.badRequest().body(new ResponseData(null, false, message));
        }
    }

    private boolean hasRequesterPermission(String permissionCode) {
        Long requesterRoleId = getRequesterRoleId();
        if (requesterRoleId == null || !StringUtils.hasText(permissionCode)) {
            return false;
        }

        Module module = moduleRepository.findByName(ARTICLES_MODULE).orElse(null);
        Permissions permission = permissionRepository.findByCode(permissionCode).orElse(null);
        if (module == null || permission == null) {
            return false;
        }

        RolePermissions rolePermission = rolePermissionsRepository
                .findByRoleIdAndModuleIdAndPermissionId(requesterRoleId, module.getId(), permission.getId())
                .orElse(null);

        return !Objects.isNull(rolePermission) && rolePermission.isEnabled();
    }

    private Long getRequesterRoleId() {
        Object roleAttribute = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes() != null
                ? ((org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.getRequestAttributes())
                .getRequest().getAttribute(BearerTokenInterceptor.REQUEST_ROLE_ID_ATTRIBUTE)
                : null;

        return roleAttribute instanceof Number ? ((Number) roleAttribute).longValue() : null;
    }

    private boolean isRequesterAdminOrOwner() {
        Object roleAttribute = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes() != null
                ? ((org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.getRequestAttributes())
                .getRequest().getAttribute(BearerTokenInterceptor.REQUEST_ROLE_ATTRIBUTE)
                : null;

        String roleName = roleAttribute instanceof String ? ((String) roleAttribute).trim().toUpperCase() : null;
        return Role.ADMIN.equals(roleName) || Role.OWNER.equals(roleName);
    }
}

