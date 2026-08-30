package com.example.scaffold.domain.Audits;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="repair_audit")
@Getter
@Setter
public class RepairAudit implements Serializable {

	private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    @Column(nullable = false)
    private Long repairId;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private Long warehouseId;

    @Column(name = "article_ids")
    private String articleIds;

    @Column(nullable = false)
    private Long userId;

    @Column
    private String description;

    @Column(name = "action_type", nullable = false)
    private String actionType;

    @Column(name="creation_date", nullable = false)
    private LocalDateTime creationDate;
}
