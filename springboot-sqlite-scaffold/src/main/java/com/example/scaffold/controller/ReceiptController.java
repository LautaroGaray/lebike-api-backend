package com.example.scaffold.controller;

import com.example.scaffold.dto.ResponseData;
import com.example.scaffold.dto.document.ReceiptCreateRequestDTO;
import com.example.scaffold.dto.document.ReceiptResponseDTO;
import com.example.scaffold.dto.document.ReceiptStatusLogResponseDTO;
import com.example.scaffold.service.auths.UserAuthorizationService;
import com.example.scaffold.service.document.ReceiptService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/receipts")
public class ReceiptController {

    private final ReceiptService receiptService;
    private final UserAuthorizationService authorizationService;

    public ReceiptController(ReceiptService receiptService,
                             UserAuthorizationService authorizationService) {
        this.receiptService = receiptService;
        this.authorizationService = authorizationService;
    }

    @PostMapping("/register")
    public ResponseEntity<ResponseData> register(@RequestBody ReceiptCreateRequestDTO request) {
        try {
            ReceiptResponseDTO created = receiptService.createReceiptWithDetails(request);
            return ResponseEntity.ok(new ResponseData(created, true, "Receipt created successfully"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ResponseData(null, false, ex.getMessage()));
        }
    }

    @PutMapping("/edit/{receiptId}")
    public ResponseEntity<ResponseData> edit(@PathVariable Long receiptId,
                                             @RequestBody ReceiptCreateRequestDTO request) {
        try {
            ReceiptResponseDTO updated = receiptService.updateReceiptWithDetails(receiptId, request);
            return ResponseEntity.ok(new ResponseData(updated, true, "Receipt updated successfully"));
        } catch (IllegalArgumentException ex) {
            String message = ex.getMessage();
            if ("Receipt not found".equals(message)) {
                return ResponseEntity.status(404).body(new ResponseData(null, false, message));
            }
            return ResponseEntity.badRequest().body(new ResponseData(null, false, message));
        }
    }

    @DeleteMapping("/delete/{receiptId}")
    public ResponseEntity<ResponseData> delete(@PathVariable Long receiptId) {
        try {
            receiptService.deleteReceipt(receiptId, authorizationService.getRequesterUserId());
            return ResponseEntity.ok(new ResponseData(null, true, "Receipt deleted successfully"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(new ResponseData(null, false, ex.getMessage()));
        }
    }

    @GetMapping("/findByUserAndWarehouse")
    public ResponseEntity<ResponseData> findByUserAndWarehouse(@RequestParam Long userId,
                                                               @RequestParam String warehouseCode) {
        try {
            List<ReceiptResponseDTO> receipts = receiptService.findByUserAndWarehouse(userId, warehouseCode);
            return ResponseEntity.ok(new ResponseData(receipts, true, "Receipts retrieved successfully"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ResponseData(null, false, ex.getMessage()));
        }
    }

    @GetMapping("/history/{receiptId}")
    public ResponseEntity<ResponseData> history(@PathVariable Long receiptId) {
        try {
            List<ReceiptStatusLogResponseDTO> history = receiptService.findStatusHistory(receiptId);
            return ResponseEntity.ok(new ResponseData(history, true, "Receipt history retrieved successfully"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(new ResponseData(null, false, ex.getMessage()));
        }
    }

    @GetMapping("/findAll")
    public ResponseEntity<ResponseData> findAll() {

        List<ReceiptResponseDTO> receipts = receiptService.findAll();
        return ResponseEntity.ok(new ResponseData(receipts, true, "Receipts retrieved successfully"));
    }
}

