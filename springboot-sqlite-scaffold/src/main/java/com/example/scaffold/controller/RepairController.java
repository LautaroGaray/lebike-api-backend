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
            RepairResponseDTO created = repairService.create(request, authorizationService.getRequesterUserId());
            return ResponseEntity.ok(new ResponseData(created, true, "Repair created successfully"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ResponseData(null, false, ex.getMessage()));
        }
    }

    @PutMapping("/edit/{repairId}")
    public ResponseEntity<ResponseData> edit(@PathVariable Long repairId,
                                             @RequestBody RepairRequestDTO request) {
        try {
            RepairResponseDTO updated = repairService.update(repairId, request, authorizationService.getRequesterUserId());
            return ResponseEntity.ok(new ResponseData(updated, true, "Repair updated successfully"));
        } catch (IllegalArgumentException ex) {
            String msg = ex.getMessage();
            if ("Repair not found".equals(msg)) {
                return ResponseEntity.status(404).body(new ResponseData(null, false, msg));
            }
            return ResponseEntity.badRequest().body(new ResponseData(null, false, msg));
        }
    }

    @DeleteMapping("/delete/{repairId}")
    public ResponseEntity<ResponseData> delete(@PathVariable Long repairId) {
        try {
            repairService.delete(repairId, authorizationService.getRequesterUserId());
            return ResponseEntity.ok(new ResponseData(null, true, "Repair deleted successfully"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(new ResponseData(null, false, ex.getMessage()));
        }
    }

    @GetMapping("/findAll")
    public ResponseEntity<ResponseData> findAll() {
        List<RepairResponseDTO> repairs = repairService.findAll();
        return ResponseEntity.ok(new ResponseData(repairs, true, "Repairs retrieved successfully"));
    }

    @GetMapping("/history/{repairId}")
    public ResponseEntity<ResponseData> history(@PathVariable Long repairId) {

        try {
            List<RepairAuditResponseDTO> history = repairService.findHistory(repairId);
            return ResponseEntity.ok(new ResponseData(history, true, "Repair history retrieved successfully"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(new ResponseData(null, false, ex.getMessage()));
        }
    }
}

