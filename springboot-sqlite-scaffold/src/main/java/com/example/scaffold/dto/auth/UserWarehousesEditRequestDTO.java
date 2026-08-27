package com.example.scaffold.dto.auth;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class UserWarehousesEditRequestDTO {
    private String email;
    private String nickName;
    private List<Long> warehouseIds;
}

