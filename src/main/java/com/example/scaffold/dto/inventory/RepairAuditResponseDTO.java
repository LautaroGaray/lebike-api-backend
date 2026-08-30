package com.example.scaffold.dto.inventory;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class RepairAuditResponseDTO {
    private Long id;
    private Long repairId;
    private String description;
    private String actionType;
    private BigDecimal price;
    private Long warehouseId;
    private List<Long> articleIds = new ArrayList<>();
    private Long userId;
    private LocalDateTime creationDate;
}
