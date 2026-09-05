package com.example.scaffold.dto.document;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ReceiptDetailResponseDTO {
    private Long id;
    private Long articleId;
    private String articleSku;
    private String articleName;
    private String supplier;
    private BigDecimal salePrice;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal purchasePrice;
    private LocalDateTime creationDate;
    private LocalDateTime editDate;
}
