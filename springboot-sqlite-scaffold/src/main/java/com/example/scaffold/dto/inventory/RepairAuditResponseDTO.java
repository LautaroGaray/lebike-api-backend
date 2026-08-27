package com.example.scaffold.dto.inventory;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class RepairAuditResponseDTO {
    private Long id;
    private Long repairId;
    private Integer status;
    private String statusDescription;
    private String description;
    private String actionType;
    private BigDecimal price;
    private Long warehouseId;
    private Long articleId;
    private Long userId;
    private LocalDateTime creationDate;
}

