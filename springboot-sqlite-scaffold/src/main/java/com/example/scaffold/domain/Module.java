package com.example.scaffold.domain;

import com.example.scaffold.util.ModuleCodeGenerator;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "modules", uniqueConstraints = {
    @UniqueConstraint(name = "UK_modules_main_id", columnNames = "main_id"),
    @UniqueConstraint(name = "UK_modules_name", columnNames = "name")
})
public class Module {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "main_id", nullable = false, length = 64)
    private String mainId;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "parent_id")
    private Long parentId;

    @Version
    private Long version;

    @Transient
    private List<Module> children = new ArrayList<>();

    @Transient
    private Map<String, Boolean> permissions = new LinkedHashMap<>();

    public Module() {
    }

    @PrePersist
    protected void generateMainId() {
        if (this.mainId == null) {
            this.mainId = ModuleCodeGenerator.generate();
        }
    }

    public Long getId() {
        return id;
    }

    public String getMainId() {
        return mainId;
    }

    public void setMainId(String mainId) {
        this.mainId = mainId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public List<Module> getChildren() {
        return children;
    }

    public void setChildren(List<Module> children) {
        this.children = children;
    }

    public Map<String, Boolean> getPermissions() {
        return permissions;
    }

    public void setPermissions(Map<String, Boolean> permissions) {
        this.permissions = permissions;
    }
}
