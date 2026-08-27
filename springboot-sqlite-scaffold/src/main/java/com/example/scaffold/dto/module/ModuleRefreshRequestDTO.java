package com.example.scaffold.dto.module;

import com.example.scaffold.domain.context.Module;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ModuleRefreshRequestDTO {
    private String email;
    private Long moduleId;
    private String moduleMainId;
    private Module module;
}

