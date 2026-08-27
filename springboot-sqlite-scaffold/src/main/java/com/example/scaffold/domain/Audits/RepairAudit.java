package com.example.scaffold.domain.Audits;

import com.example.scaffold.domain.context.Status;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="repair_audit")
@Getter
@Setter
public class RepairAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    @Column(nullable = false)
    private Long repairId;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private Long warehouseId;

    @Column(nullable = true)
    private Long articleId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Integer status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status", referencedColumnName = "status", insertable = false, updatable = false)
    private Status statusInfo;

    @Column
    private String description;

    @Column(name = "action_type", nullable = false)
    private String actionType;

    @Column(name="creation_date", nullable = false)
    private LocalDateTime creationDate;
}
