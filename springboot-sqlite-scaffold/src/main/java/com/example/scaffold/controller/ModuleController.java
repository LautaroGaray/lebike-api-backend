package com.example.scaffold.controller;

import com.example.scaffold.domain.Module;
import com.example.scaffold.dto.ResponseData;
import com.example.scaffold.dto.auth.UserDTO;
import com.example.scaffold.dto.module.ModuleRefreshRequestDTO;
import com.example.scaffold.service.ModuleService;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/modules")
public class ModuleController {

    private final ModuleService moduleService;

    public ModuleController(ModuleService moduleService) {
        this.moduleService = moduleService;
    }

    @PostMapping("/loadByUser")
    public ResponseEntity<ResponseData> loadByUser(@RequestBody UserDTO userDTO) {
        if (Objects.isNull(userDTO) || !StringUtils.hasText(userDTO.getEmail())) {
            return ResponseEntity.badRequest().body(new ResponseData(null, false, "Email is required"));
        }

        List<Module> modules = moduleService.loadModulesByUser(userDTO).orElse(null);
        if (modules == null) {
            return ResponseEntity.status(404).body(new ResponseData(null, false, "User not found or without modules"));
        }

        return ResponseEntity.ok(new ResponseData(modules, true, "Modules loaded successfully"));
    }

    @PostMapping("/refreshViewed")
    public ResponseEntity<ResponseData> refreshViewedModule(@RequestBody ModuleRefreshRequestDTO request) {
        if (Objects.isNull(request) || !StringUtils.hasText(request.getEmail())) {
            return ResponseEntity.badRequest().body(new ResponseData(null, false, "Email is required"));
        }

        Long moduleId = request.getModuleId();
        String moduleMainId = request.getModuleMainId();

        if (request.getModule() != null) {
            if (moduleId == null) {
                moduleId = request.getModule().getId();
            }
            if (!StringUtils.hasText(moduleMainId)) {
                moduleMainId = request.getModule().getMainId();
            }
        }

        if (moduleId == null && !StringUtils.hasText(moduleMainId)) {
            return ResponseEntity.badRequest().body(new ResponseData(null, false, "moduleId or moduleMainId is required"));
        }

        UserDTO userDTO = new UserDTO();
        userDTO.setEmail(request.getEmail());

        Module refreshedModule = moduleService
                .reloadModuleWithChildrenByUser(userDTO, moduleId, moduleMainId)
                .orElse(null);

        if (refreshedModule == null) {
            return ResponseEntity.status(403).body(new ResponseData(null, false, "Access denied or module not found after refresh"));
        }

        return ResponseEntity.ok(new ResponseData(refreshedModule, true, "Module access refreshed successfully"));
    }
}

