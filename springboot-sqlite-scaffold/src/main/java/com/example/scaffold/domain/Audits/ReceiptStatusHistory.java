package com.example.scaffold.domain.Audits;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name="receipt_status_history")
public class ReceiptStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    @Column(name = "receipt_id", nullable = false)
    private Long receiptId;

    @Column(nullable = false)
    private Integer status;

    @Column
    private String description;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_email")
    private String userEmail;

    @Column(nullable = false, updatable = false, name = "creation_date")
    private LocalDateTime creationDate;
}
