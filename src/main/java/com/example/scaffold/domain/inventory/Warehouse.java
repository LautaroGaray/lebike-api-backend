package com.example.scaffold.domain.inventory;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name="warehouse",
        indexes= {
            @Index(
                    name="idx_warehouse_code",
                    columnList = "code",
                    unique = true
            )
        }
)
public class Warehouse implements Serializable {

	private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    @Column(name="code", nullable = false, unique = true)
    public String code;
    /*----------
    Warehouses code and name

    1 - Berazategui
    2 - Ezpeleta
    3 - Bernal
     */

    @Column(name="name")
    public String name;

    @Column(name="active")
    public boolean active;

    @Column(name="creation_date", nullable = false)
    public LocalDateTime creationDate;

    @Column(name="editDate")
    public LocalDateTime editDate;
}
