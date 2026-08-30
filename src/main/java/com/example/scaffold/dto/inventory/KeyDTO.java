package com.example.scaffold.dto.inventory;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KeyDTO {

    private Long Id;
    private Integer oldNumberKey;
    private Integer newNumberKey;
    private String oldLetterKey;
    private String newLetterKey;
    private String prefix;
    private String targetDestiny;
    public String completKey;
}
