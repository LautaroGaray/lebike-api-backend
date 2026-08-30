package com.example.scaffold.dto.document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ReceiptResponseDTO {
    private Long id;
    private String receiptKey;
    private Integer status;
    private String statusDescription;
    private String origin;
    private String destiny;
    private String description;
    private Long userId;
    private String username;
    private String userEmail;
    private LocalDateTime creationDate;
    private LocalDateTime editDate;
    private List<ReceiptDetailResponseDTO> details = new ArrayList<>();
}

