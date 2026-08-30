package com.example.scaffold.domain.Audits;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name="key")
public class Keys {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    @Column(name = "incremental_key")
    private Integer incrementaNumberKey;

    @Column(name = "incremental_letter_key")
    private String incrementalLetterKey;

    @Column(unique = true)
    private String prefix;

    @Column(unique = true, name = "target_destiny")
    private String targetDestiny;
}
