package com.example.scaffold.domain.documents;

import com.example.scaffold.domain.inventory.Article;
import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import javax.persistence.Entity;
import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name="receipt_detail")
public class ReceiptDetail implements Serializable {

	private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name ="receipt_id", nullable = false)
    private Receipt receipt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @Column(name="creation_date", nullable = false, updatable = false)
    private LocalDateTime creationDate;

    @Column(name="edit_date")
    private LocalDateTime editDate;
}
