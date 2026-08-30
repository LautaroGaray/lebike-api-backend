package com.example.scaffold.dto.document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ReceiptDetailResponseDTO {
    private Long id;
    private Long articleId;
    private String articleSku;
    private String articleName;
    private LocalDateTime creationDate;
    private LocalDateTime editDate;
}

