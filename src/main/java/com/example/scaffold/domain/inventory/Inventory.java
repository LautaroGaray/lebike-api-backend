package com.example.scaffold.domain.inventory;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name="inventory")
public class Inventory implements Serializable {

	private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    public Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    public Article article;

    @Column(nullable = false)
    public Integer quantity;

    @Column(name = "creation_date", nullable = false)
    public LocalDateTime creationDate;

    @Column(name = "edit_date")
    public LocalDateTime editDate;
}
