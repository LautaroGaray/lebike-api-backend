package com.example.scaffold.dto.inventory;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class RepairRequestDTO {
    private Long warehouseId;
    private Long articleId;
    private BigDecimal price;
    private Integer status;
    private String description;
}

