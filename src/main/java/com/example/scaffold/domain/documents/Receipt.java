package com.example.scaffold.domain.documents;

import com.example.scaffold.domain.auths.Users;
import com.example.scaffold.domain.context.Status;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name="receipt")
public class Receipt implements Serializable {

	private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    @Column(unique = true, name = "receiptKey")
    private String receiptKey;

    @Column(name = "status", nullable = false)
    private Integer status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status", referencedColumnName = "status", insertable = false, updatable = false)
    private Status statusInfo;

    @Column(name="origin", nullable = false)
    private String origin;

    @Column(name = "destiny", nullable = false)
    private String destiny;

    @Column(name="description")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="user_id", nullable = false)
    private Users user;

    @Column(name="creation_date", nullable = false, updatable = false)
    private LocalDateTime creationDate;

    @Column(name="edit_date")
    private LocalDateTime editDate;

    @OneToMany(
            mappedBy = "receipt",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ReceiptDetail> detaile = new ArrayList<>();
}
