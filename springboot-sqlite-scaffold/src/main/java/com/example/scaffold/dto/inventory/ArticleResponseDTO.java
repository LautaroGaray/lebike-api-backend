package com.example.scaffold.dto.inventory;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class ArticleResponseDTO {
    private Long id;
    private String sku;
    private String name;
    private String type;
    private String supplier;
    private BigDecimal purchasePrice;
    private BigDecimal salePrice;
    private boolean active;
}

