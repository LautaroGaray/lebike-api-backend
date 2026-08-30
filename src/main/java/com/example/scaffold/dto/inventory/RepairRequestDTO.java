package com.example.scaffold.dto.inventory;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class RepairRequestDTO {
    private Long warehouseId;
    private List<Long> articleIds = new ArrayList<>();
    private BigDecimal price;
    private String description;
}
