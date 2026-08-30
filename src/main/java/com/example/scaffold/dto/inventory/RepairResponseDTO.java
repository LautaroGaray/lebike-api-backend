package com.example.scaffold.dto.inventory;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class RepairResponseDTO {
    private Long id;
    private String description;
    private BigDecimal price;
    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private List<RepairArticleDTO> articles = new ArrayList<>();
    private Long userId;
    private String username;
    private String userEmail;
    private LocalDateTime creationDate;
    private LocalDateTime editDate;
}
