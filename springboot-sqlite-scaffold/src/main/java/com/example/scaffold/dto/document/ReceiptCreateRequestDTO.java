package com.example.scaffold.dto.document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ReceiptCreateRequestDTO {
    private Long userId;
    private Integer status;
    private String origin;
    private String destiny;
    private String description;
    private List<ReceiptDetailCreateRequestDTO> details = new ArrayList<>();
}

