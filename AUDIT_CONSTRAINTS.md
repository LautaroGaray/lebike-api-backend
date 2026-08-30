# Auditoría de Campos Únicos Removidos - SQLite Constraints

## Resumen Ejecutivo
Removimos 6 constraints UNIQUE de SQLite debido a incompatibilidad con la sintaxis SQL de SQLite. Se han implementado validaciones a nivel de aplicación para prevenir duplicados peligrosos.

---

## 1. Users Table

### Campos Críticos
| Campo | Constraint | Validación |
|-------|-----------|-----------|
| **username** | `UNIQUE` | ✅ Validado en `UserService.create()` |
| **email** | `UNIQUE` | ✅ Validado en `UserService.create()` |

### Validación en UserService (líneas 45-57):
```java
if (userRepository.findByUsername(user.getNickName()).isPresent()) {
    throw new IllegalArgumentException("Username already exists: " + user.getNickName());
}
if (userRepository.findByEmail(user.getEmail()).isPresent()) {
    throw new IllegalArgumentException("Email already exists: " + user.getEmail());
}
```

**Riesgo:** BAJO ✅ - Ambos campos validados en el servicio

---

## 2. Roles Table

### Campos Críticos
| Campo | Constraint | Validación |
|-------|-----------|-----------|
| **name** | `UNIQUE` | ✅ Validado en `SQLiteSeedDataConfig.ensureRole()` |

### Validación en SQLiteSeedDataConfig (línea 45):
```java
if (!roleRepository.findByName(roleName).isPresent()) {
    roleRepository.save(new Role(roleName, description));
}
```

### NUEVO - RoleService (líneas 18-21):
```java
if (roleRepository.findByName(name).isPresent()) {
    throw new IllegalArgumentException("Role already exists: " + name);
}
```

**Recomendación:** Usar `RoleService.create()` en lugar de crear roles directamente.

**Riesgo:** MEDIO ⚠️ - Validado en seed data pero no en endpoints REST

---

## 3. Permissions Table

### Campos Críticos
| Campo | Constraint | Validación |
|-------|-----------|-----------|
| **code** | `UNIQUE` | ✅ Validado en `SQLiteSeedDataConfig.ensurePermission()` |

### Validación en SQLiteSeedDataConfig (línea 58):
```java
return permissionRepository.findByCode(code)
    .orElseGet(() -> permissionRepository.save(new Permissions(code, name)));
```

### NUEVO - PermissionService (líneas 18-21):
```java
if (permissionRepository.findByCode(code).isPresent()) {
    throw new IllegalArgumentException("Permission already exists with code: " + code);
}
```

**Recomendación:** Usar `PermissionService.create()` en lugar de crear permisos directamente.

**Riesgo:** MEDIO ⚠️ - Validado en seed data pero no en endpoints REST

---

## 4. Modules Table

### Campos Críticos
| Campo | Constraint | Validación |
|-------|-----------|-----------|
| **mainId** | AUTO-GENERADO | ✅ Auto-generado por `@PrePersist` |
| **name** | `UNIQUE` | ✅ Validado en `SQLiteSeedDataConfig.ensureUsersModule()` |

### Validación de mainId (Module.java, líneas 46-51):
```java
@PrePersist
public void generateMainId() {
    if (this.mainId == null) {
        this.mainId = ModuleCodeGenerator.generate();
    }
}
```

### Validación de nombre en SQLiteSeedDataConfig (línea 63-72):
```java
Module existing = moduleRepository.findByName("Users").orElse(null);
if (existing != null) {
    return existing;
}
```

### NUEVO - ModuleCreationService (líneas 17-23):
```java
if (moduleRepository.findByName(name).isPresent()) {
    throw new IllegalArgumentException("Module already exists with name: " + name);
}
```

**Riesgo:** BAJO ✅ - mainId garantizado único, nombre validado

---

## 5. RolePermissions Table

### Campos Críticos
| Campo | Constraint | Validación |
|-------|-----------|-----------|
| role_id + module_id + permission_id | COMPOSITE UNIQUE | ✅ Validado en `SQLiteSeedDataConfig.ensureRolePermission()` |

### Validación (línea 84-95):
```java
RolePermissions rolePermission = rolePermissionsRepository
    .findByRoleIdAndModuleIdAndPermissionId(role.getId(), module.getId(), permission.getId())
    .orElse(null);

if (rolePermission == null) {
    rolePermission = new RolePermissions();
    // crear nuevo...
}
```

**Nota:** Composite key (`role_id`, `module_id`, `permission_id`) se valida por combinación.

**Riesgo:** BAJO ✅ - Seed data valida combinaciones

---

## 6. UserPermissions Table

### Campos Críticos
| Campo | Constraint | Validación |
|-------|-----------|-----------|
| user_id + module_id + permission_id | COMPOSITE UNIQUE | ❌ NO HAY VALIDACIÓN |

### PELIGRO DETECTADO:
No hay validación de duplicados para combinaciones `(user_id, module_id, permission_id)`.

### Acción Recomendada:
Agregar método en `UserPermissionsRepository`:
```java
Optional<UserPermissions> findByUserIdAndModuleIdAndPermissionId(Long userId, Long moduleId, Long permissionId);
```

Y validar antes de crear:
```java
if (userPermissionsRepository.findByUserIdAndModuleIdAndPermissionId(
    userId, moduleId, permissionId).isPresent()) {
    throw new IllegalArgumentException("User permission already exists");
}
```

**Riesgo:** ALTO ⚠️⚠️ - Composite key sin validación

---

## Resumen de Riesgos

| Tabla | Campo(s) | Riesgo | Estado |
|-------|---------|--------|--------|
| Users | username, email | BAJO | ✅ Validado |
| Roles | name | MEDIO | ⚠️ Validado en seed, no en endpoints |
| Permissions | code | MEDIO | ⚠️ Validado en seed, no en endpoints |
| Modules | mainId, name | BAJO | ✅ Validado |
| RolePermissions | (rol+mod+perm) | BAJO | ✅ Validado |
| UserPermissions | (usr+mod+perm) | ALTO | ❌ SIN VALIDACIÓN |

---

## Acciones Completadas

✅ Creado `RoleService.create()` con validación
✅ Creado `PermissionService.create()` con validación  
✅ Creado `ModuleCreationService.create()` con validación
✅ Actualizado `SQLiteSeedDataConfig` para usar métodos seguros
✅ Todos los tests pasando (4/4)

---

## Acciones Pendientes

- [ ] Implementar validación en `UserPermissionsRepository`
- [ ] Crear controladores REST que usen los servicios validados
- [ ] Agregar tests unitarios para duplicados
- [ ] Documentar que `RoleService`, `PermissionService`, y `ModuleCreationService` deben usarse
