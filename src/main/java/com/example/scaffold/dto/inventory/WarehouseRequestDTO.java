package com.example.scaffold.dto.inventory;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class WarehouseRequestDTO {
    private String code;
    private String name;
    private Boolean active;
}

