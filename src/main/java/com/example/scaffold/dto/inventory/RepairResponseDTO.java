package com.example.scaffold.dto.inventory;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class RepairResponseDTO {
    private Long id;
    private Integer status;
    private String statusDescription;
    private String description;
    private BigDecimal price;
    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private Long articleId;
    private String articleSku;
    private String articleName;
    private Long userId;
    private String username;
    private String userEmail;
    private LocalDateTime creationDate;
    private LocalDateTime editDate;
}

