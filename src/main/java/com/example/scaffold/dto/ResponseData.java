package com.example.scaffold.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResponseData {
    public Object data;
    public boolean isSuccess;
    public String message;
}
