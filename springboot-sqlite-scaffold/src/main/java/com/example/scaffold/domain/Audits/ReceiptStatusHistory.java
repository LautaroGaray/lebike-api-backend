package com.example.scaffold.domain.Audits;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name="receipt_status_history")
public class ReceiptStatusHistory implements Serializable {

	private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    @Column(name = "receipt_id", nullable = false)
    private Long receiptId;

    @Column(name="receipt_key")
    private String receiptKey;

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
