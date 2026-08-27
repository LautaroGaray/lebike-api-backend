package com.example.scaffold.domain.inventory;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name="bike_component")
public class BikeComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;


    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="article_id", nullable = false)
    public Article article;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="bike_article_id", nullable = false)
    public Article bikeArticle;

    @Column(name = "creation_date", nullable = false)
    public LocalDateTime CreationDate;

    @Column(name= "edit_date")
    public LocalDateTime editDate;
}
