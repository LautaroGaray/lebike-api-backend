package com.example.scaffold.domain.auths;

import com.example.scaffold.domain.context.Module;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import java.io.Serializable;

@Entity
@Table(name = "user_permissions", uniqueConstraints = {
    @UniqueConstraint(name = "UK_user_permissions_composite", columnNames = {"user_id", "module_id", "permission_id"})
})
public class UserPermissions implements Serializable {

	private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_permissions_user"))
    private Users user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "module_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_permissions_module"))
    private Module module;

    @ManyToOne(optional = false)
    @JoinColumn(name = "permission_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_permissions_permission"))
    private Permissions permission;

    @Column(nullable = false)
    private boolean enabled;

    @Version
    private Long version;

    protected UserPermissions() {
    }

    public Long getId() {
        return id;
    }

    public Users getUser() {
        return user;
    }

    public void setUser(Users user) {
        this.user = user;
    }

    public Module getModule() {
        return module;
    }

    public void setModule(Module module) {
        this.module = module;
    }

    public Permissions getPermission() {
        return permission;
    }

    public void setPermission(Permissions permission) {
        this.permission = permission;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}

