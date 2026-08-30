package com.example.scaffold.dto.inventory;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class WarehouseResponseDTO {
    private Long id;
    private String code;
    private String name;
    private Boolean active;
    private LocalDateTime creationDate;
    private LocalDateTime editDate;
}

