package com.example.scaffold.domain.auths;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import java.io.Serializable;

@Entity
@Table(name = "roles", uniqueConstraints = {
    @UniqueConstraint(name = "UK_roles_name", columnNames = "name")
})
public class Role implements Serializable {
	private static final long serialVersionUID = 1L;
	
    public static final String USER = "USER";
    public static final String ADMIN = "ADMIN";
    public static final String OWNER = "OWNER";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String name;

    @Column(nullable = false, length = 128)
    private String description;

    @Version
    private Long version;

    protected Role() {
    }

    public Role(String name, String description) {
        this.name = normalizeName(name);
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = normalizeName(name);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    private static String normalizeName(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}

