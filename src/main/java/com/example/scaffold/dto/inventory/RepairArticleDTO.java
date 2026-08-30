package com.example.scaffold.dto.inventory;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RepairArticleDTO {
    private Long id;
    private String sku;
    private String name;
}

