package com.example.scaffold.domain.documents;

import com.example.scaffold.domain.context.Status;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "receipt_status_log")
public class ReceiptStatusLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receipt_id", nullable = false)
    private Long receiptId;

    @Column(name = "receipt_key")
    private String receiptKey;

    @Column(name = "previous_status")
    private Integer previousStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_status", referencedColumnName = "status", insertable = false, updatable = false)
    private Status previousStatusInfo;

    @Column(name = "new_status", nullable = false)
    private Integer newStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_status", referencedColumnName = "status", insertable = false, updatable = false)
    private Status newStatusInfo;

    @Column(name = "origin", nullable = false)
    private String origin;

    @Column(name = "destiny", nullable = false)
    private String destiny;

    @Column(name = "description")
    private String description;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_email")
    private String userEmail;

    @Column(name = "receipt_creation_date")
    private LocalDateTime receiptCreationDate;

    @Column(name = "receipt_edit_date")
    private LocalDateTime receiptEditDate;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;
}

