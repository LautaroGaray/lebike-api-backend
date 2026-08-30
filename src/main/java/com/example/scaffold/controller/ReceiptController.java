package com.example.scaffold.controller;

import com.example.scaffold.dto.ResponseData;
import com.example.scaffold.dto.document.ReceiptCreateRequestDTO;
import com.example.scaffold.dto.document.ReceiptUpdateRequestDTO;
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
            Long requesterId = authorizationService.getRequesterUserId();
            ReceiptResponseDTO created = receiptService.createReceiptWithDetails(request, requesterId);
            return ResponseEntity.ok(new ResponseData(created, true, "Receipt created successfully"));
        } catch (IllegalArgumentException ex) {
            return toReceiptErrorResponse(ex);
        }
    }

    @PutMapping("/edit/{receiptId}")
    public ResponseEntity<ResponseData> edit(@PathVariable Long receiptId,
                                             @RequestBody ReceiptUpdateRequestDTO request) {
        try {
            Long requesterId = authorizationService.getRequesterUserId();
            ReceiptResponseDTO updated = receiptService.updateReceiptWithDetails(receiptId, request, requesterId);
            return ResponseEntity.ok(new ResponseData(updated, true, "Receipt updated successfully"));
        } catch (IllegalArgumentException ex) {
            return toReceiptErrorResponse(ex);
        }
    }

    @DeleteMapping("/delete/{receiptId}")
    public ResponseEntity<ResponseData> delete(@PathVariable Long receiptId) {
        try {
            receiptService.deleteReceipt(receiptId, authorizationService.getRequesterUserId());
            return ResponseEntity.ok(new ResponseData(null, true, "Receipt deleted successfully"));
        } catch (IllegalArgumentException ex) {
            return toReceiptErrorResponse(ex);
        }
    }

    @GetMapping("/findByUserAndWarehouse")
    public ResponseEntity<ResponseData> findByUserAndWarehouse(@RequestParam Long userId,
                                                               @RequestParam String warehouseCode) {
        try {
            Long requesterId = authorizationService.getRequesterUserId();
            List<ReceiptResponseDTO> receipts = receiptService.findByUserAndWarehouse(userId, warehouseCode, requesterId);
            return ResponseEntity.ok(new ResponseData(receipts, true, "Receipts retrieved successfully"));
        } catch (IllegalArgumentException ex) {
            return toReceiptErrorResponse(ex);
        }
    }

    @GetMapping("/history/{receiptId}")
    public ResponseEntity<ResponseData> history(@PathVariable Long receiptId) {
        try {
            Long requesterId = authorizationService.getRequesterUserId();
            List<ReceiptStatusLogResponseDTO> history = receiptService.findStatusHistory(receiptId, requesterId);
            return ResponseEntity.ok(new ResponseData(history, true, "Receipt history retrieved successfully"));
        } catch (IllegalArgumentException ex) {
            return toReceiptErrorResponse(ex);
        }
    }

    @GetMapping("/findAll")
    public ResponseEntity<ResponseData> findAll() {
		try {
			Long requesterId = authorizationService.getRequesterUserId();
			if (requesterId == null) {
				return ResponseEntity.status(401).body(new ResponseData(null, false, "Requester user is not authenticated"));
			}
			List<ReceiptResponseDTO> receipts = receiptService.findAll(requesterId);
			return ResponseEntity.ok(new ResponseData(receipts, true, "Receipts retrieved successfully"));
		} catch (IllegalArgumentException ex) {
			return toReceiptErrorResponse(ex);
		}
     }

	 private ResponseEntity<ResponseData> toReceiptErrorResponse(IllegalArgumentException ex) {
		String message = ex.getMessage() == null ? "Invalid receipt request" : ex.getMessage();
		if ("Requester user is not authenticated".equals(message)) {
			return ResponseEntity.status(401).body(new ResponseData(null, false, message));
		}
		if ("Requester user not found".equals(message) || "Receipt not found".equals(message)) {
			return ResponseEntity.status(404).body(new ResponseData(null, false, message));
		}
		if ("Receipt access denied for requester scope".equals(message)) {
			return ResponseEntity.status(403).body(new ResponseData(null, false, message));
		}
		return ResponseEntity.badRequest().body(new ResponseData(null, false, message));
	 }
  }
