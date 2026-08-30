package com.example.scaffold.domain.inventory;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name="article",
       uniqueConstraints = {
        @UniqueConstraint(
            name="uk_sku_supplier",
            columnNames ={"sku","supplier"}
            )
        }
)
public class Article implements Serializable {

	private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    @Column(name="sku", nullable = false, unique = true)
    private String sku;

    @Column(name="external_sku", unique = true)
    private String externalSku;

    @Column(name="name", nullable = false)
    private String name;

    @Column(name="type")
    private String type;

    @Column(name="supplier")
    private String supplier;

    @Column(name="purchase_price", precision = 12, scale = 12, nullable = false)
    private BigDecimal purchasePrice;

    @Column(name="sale_price", precision = 12, scale = 12, nullable = false)
    private BigDecimal salePrice;


}
