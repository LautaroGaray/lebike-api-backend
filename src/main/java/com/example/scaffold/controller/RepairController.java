package com.example.scaffold.controller;

import com.example.scaffold.dto.ResponseData;
import com.example.scaffold.dto.inventory.RepairAuditResponseDTO;
import com.example.scaffold.dto.inventory.RepairRequestDTO;
import com.example.scaffold.dto.inventory.RepairResponseDTO;
import com.example.scaffold.service.auths.UserAuthorizationService;
import com.example.scaffold.service.inventory.RepairService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/repairs")
public class RepairController {

    private final RepairService repairService;
    private final UserAuthorizationService authorizationService;

    public RepairController(RepairService repairService,
                            UserAuthorizationService authorizationService) {
        this.repairService = repairService;
        this.authorizationService = authorizationService;
    }

    @PostMapping("/register")
    public ResponseEntity<ResponseData> register(@RequestBody RepairRequestDTO request) {
        try {
            Long requesterId = authorizationService.getRequesterUserId();
            RepairResponseDTO created = repairService.create(request, requesterId);
            return ResponseEntity.ok(new ResponseData(created, true, "Repair created successfully"));
        } catch (IllegalArgumentException ex) {
            return toRepairErrorResponse(ex);
        }
    }

    @PutMapping("/edit/{repairId}")
    public ResponseEntity<ResponseData> edit(@PathVariable Long repairId,
                                             @RequestBody RepairRequestDTO request) {
        try {
            Long requesterId = authorizationService.getRequesterUserId();
            RepairResponseDTO updated = repairService.update(repairId, request, requesterId);
            return ResponseEntity.ok(new ResponseData(updated, true, "Repair updated successfully"));
        } catch (IllegalArgumentException ex) {
            return toRepairErrorResponse(ex);
        }
    }

    @DeleteMapping("/delete/{repairId}")
    public ResponseEntity<ResponseData> delete(@PathVariable Long repairId) {
        try {
            repairService.delete(repairId, authorizationService.getRequesterUserId());
            return ResponseEntity.ok(new ResponseData(null, true, "Repair deleted successfully"));
        } catch (IllegalArgumentException ex) {
            return toRepairErrorResponse(ex);
        }
    }

    @GetMapping("/findAll")
    public ResponseEntity<ResponseData> findAll() {
        try {
            Long requesterId = authorizationService.getRequesterUserId();
            List<RepairResponseDTO> repairs = repairService.findAll(requesterId);
            return ResponseEntity.ok(new ResponseData(repairs, true, "Repairs retrieved successfully"));
        } catch (IllegalArgumentException ex) {
            return toRepairErrorResponse(ex);
        }
    }

    @GetMapping("/findByWarehouse")
    public ResponseEntity<ResponseData> findByWarehouse(@RequestParam String warehouseCode) {
        try {
            Long requesterId = authorizationService.getRequesterUserId();
            List<RepairResponseDTO> repairs = repairService.findByWarehouse(warehouseCode, requesterId);
            return ResponseEntity.ok(new ResponseData(repairs, true, "Repairs retrieved successfully"));
        } catch (IllegalArgumentException ex) {
            return toRepairErrorResponse(ex);
        }
    }

    @GetMapping("/history/{repairId}")
    public ResponseEntity<ResponseData> history(@PathVariable Long repairId) {

        try {
            Long requesterId = authorizationService.getRequesterUserId();
            List<RepairAuditResponseDTO> history = repairService.findHistory(repairId, requesterId);
            return ResponseEntity.ok(new ResponseData(history, true, "Repair history retrieved successfully"));
        } catch (IllegalArgumentException ex) {
            return toRepairErrorResponse(ex);
        }
    }

    private ResponseEntity<ResponseData> toRepairErrorResponse(IllegalArgumentException ex) {
        String message = ex.getMessage() == null ? "Invalid repair request" : ex.getMessage();

        if ("Requester user is not authenticated".equals(message)) {
            return ResponseEntity.status(401).body(new ResponseData(null, false, message));
        }

        if ("Requester user not found".equals(message) || "Repair not found".equals(message)) {
            return ResponseEntity.status(404).body(new ResponseData(null, false, message));
        }

        if (message.contains("access denied") || message.contains("not allowed for USER")) {
            return ResponseEntity.status(403).body(new ResponseData(null, false, message));
        }

        return ResponseEntity.badRequest().body(new ResponseData(null, false, message));
    }
}
