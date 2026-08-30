package com.example.scaffold.controller;

import com.example.scaffold.dto.ResponseData;
import com.example.scaffold.dto.inventory.WarehouseRequestDTO;
import com.example.scaffold.dto.inventory.WarehouseResponseDTO;
import com.example.scaffold.service.auths.UserAuthorizationService;
import com.example.scaffold.service.inventory.WarehouseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/warehouses")
public class WarehouseController {

    private final WarehouseService warehouseService;
    private final UserAuthorizationService authorizationService;

    public WarehouseController(WarehouseService warehouseService,
                               UserAuthorizationService authorizationService) {
        this.warehouseService = warehouseService;
        this.authorizationService = authorizationService;
    }

    @PostMapping("/register")
    public ResponseEntity<ResponseData> register(@RequestBody WarehouseRequestDTO request) {
        try {
            WarehouseResponseDTO created = warehouseService.create(request, authorizationService.getRequesterUserId());
            return ResponseEntity.ok(new ResponseData(created, true, "Warehouse created successfully"));
        } catch (IllegalArgumentException ex) {
            return toWarehouseErrorResponse(ex);
        }
    }

    @PutMapping("/edit/{warehouseId}")
    public ResponseEntity<ResponseData> edit(@PathVariable Long warehouseId,
                                             @RequestBody WarehouseRequestDTO request) {
        try {
            WarehouseResponseDTO updated = warehouseService.update(warehouseId, request, authorizationService.getRequesterUserId());
            return ResponseEntity.ok(new ResponseData(updated, true, "Warehouse updated successfully"));
        } catch (IllegalArgumentException ex) {
            return toWarehouseErrorResponse(ex);
        }
    }

    @DeleteMapping("/delete/{warehouseId}")
    public ResponseEntity<ResponseData> delete(@PathVariable Long warehouseId) {
        try {
            warehouseService.delete(warehouseId, authorizationService.getRequesterUserId());
            return ResponseEntity.ok(new ResponseData(null, true, "Warehouse deleted successfully"));
        } catch (IllegalArgumentException ex) {
            return toWarehouseErrorResponse(ex);
        }
    }

    @GetMapping("/findAll")
    public ResponseEntity<ResponseData> findAll() {
        try {
            List<WarehouseResponseDTO> warehouses = warehouseService.findVisible(authorizationService.getRequesterUserId());
            return ResponseEntity.ok(new ResponseData(warehouses, true, "Warehouses retrieved successfully"));
        } catch (IllegalArgumentException ex) {
            return toWarehouseErrorResponse(ex);
        }
    }

    @GetMapping("/find/{warehouseId}")
    public ResponseEntity<ResponseData> findById(@PathVariable Long warehouseId) {
        try {
            WarehouseResponseDTO warehouse = warehouseService.findVisibleById(warehouseId, authorizationService.getRequesterUserId());
            return ResponseEntity.ok(new ResponseData(warehouse, true, "Warehouse retrieved successfully"));
        } catch (IllegalArgumentException ex) {
            return toWarehouseErrorResponse(ex);
        }
    }

    private ResponseEntity<ResponseData> toWarehouseErrorResponse(IllegalArgumentException ex) {
        String message = ex.getMessage() == null ? "Invalid warehouse request" : ex.getMessage();

        if (message.startsWith("Only OWNER") || message.startsWith("User cannot access this warehouse")) {
            return ResponseEntity.status(403).body(new ResponseData(null, false, message));
        }

        if (message.startsWith("Requester user is not authenticated")) {
            return ResponseEntity.status(401).body(new ResponseData(null, false, message));
        }

        if ("Warehouse not found".equals(message)) {
            return ResponseEntity.status(404).body(new ResponseData(null, false, message));
        }

        return ResponseEntity.badRequest().body(new ResponseData(null, false, message));
    }
}
