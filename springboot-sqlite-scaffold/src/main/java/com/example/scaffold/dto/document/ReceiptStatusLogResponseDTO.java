package com.example.scaffold.dto.document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ReceiptStatusLogResponseDTO {
    private Long id;
    private Long receiptId;
    private Integer previousStatus;
    private String previousStatusDescription;
    private Integer newStatus;
    private String newStatusDescription;
    private String origin;
    private String destiny;
    private String description;
    private Long userId;
    private String userEmail;
    private LocalDateTime receiptCreationDate;
    private LocalDateTime receiptEditDate;
    private LocalDateTime changedAt;
}

