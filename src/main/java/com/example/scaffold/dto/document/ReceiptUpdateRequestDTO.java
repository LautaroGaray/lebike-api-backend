package com.example.scaffold.dto.document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ReceiptUpdateRequestDTO {
    private Integer status;
    private List<ReceiptDetailCreateRequestDTO> details = new ArrayList<>();
}

