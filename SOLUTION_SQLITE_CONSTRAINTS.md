# Solución Final: SQLite Constraints y Database Integrity

## Problema Original
SQLite no soporta la sintaxis `ALTER TABLE ADD CONSTRAINT UNIQUE` que Hibernate genera automáticamente. Esto causaba errores al iniciar la aplicación.

## Solución Implementada

### 1. **UNIQUE Constraints en CREATE TABLE** (via Annotations)
Las `@UniqueConstraint` annotations en las entidades JPA se incluyen correctamente en el CREATE TABLE generado por Hibernate. Esto funciona en SQLite porque los constraints se definen en la creación de la tabla, no con ALTER TABLE.

**Entidades actualizado**:
- ✅ `Users` - username, email
- ✅ `Role` - name
- ✅ `Permissions` - code
- ✅ `Module` - mainId, name
- ✅ `RolePermissions` - (role_id, module_id, permission_id)
- ✅ `UserPermissions` - (user_id, module_id, permission_id)

### 2. **SQLiteDialect Configuration**
```java
@Override
public boolean supportsUniqueConstraintInCreateAlterTable() {
    return false;  // ← Previene que Hibernate intente ALTER TABLE
}

@Override
public String getAddUniqueConstraintString(String constraintName) {
    return "";  // ← Asegura que no hay intento de ADD CONSTRAINT
}
```

**Por qué funciona:**
- Cuando retorna `false`, Hibernate NO intenta hacer ALTER TABLE para agregar constraints
- Los constraints en `@UniqueConstraint` se incluyen en CREATE TABLE (soportado en SQLite)
- Los constraints que están en CREATE TABLE NO necesitan ALTER TABLE

### 3. **UNIQUE Indexes para Garantía Extra**
SQLite puede usar índices UNIQUE como equivalente a constraints UNIQUE. Agregamos un archivo `schema-sqlite.sql` que se ejecuta automáticamente después de que Hibernate crea las tablas:

```sql
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE UNIQUE INDEX IF NOT EXISTS idx_roles_name ON roles(name);
CREATE UNIQUE INDEX IF NOT EXISTS idx_permissions_code ON permissions(code);
CREATE UNIQUE INDEX IF NOT EXISTS idx_modules_main_id ON modules(main_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_modules_name ON modules(name);
CREATE UNIQUE INDEX IF NOT EXISTS idx_role_permissions_unique ON role_permissions(role_id, module_id, permission_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_permissions_unique ON user_permissions(user_id, module_id, permission_id);
```

**Ventajas:**
- Los índices UNIQUE fuerzan la unicidad automáticamente en SQLite
- Son redundantes con los constraints, pero proporcionan capas adicionales de protección
- También mejoran la performance de búsquedas

### 4. **Regular Indexes para Performance**
Se agregaron índices en columnas de clave foránea y búsquedas frecuentes:
- Foreign keys: `role_id`, `user_id`, `module_id`, `permission_id`, `parent_id`
- Búsquedas comunes: `email`, `username`, `name`, `code`, `main_id`

### 5. **Application-Level Validation**
Se mantienen validaciones en los servicios como defensa en profundidad:
- `UserService.create()` - Valida username y email
- `RoleService.create()` - Valida nombre de rol
- `PermissionService.create()` - Valida código de permiso
- `ModuleCreationService.create()` - Valida nombre de módulo

**Por qué es importante:**
- Proporciona mensajes de error claros y amigables
- Permite detectar duplicados antes de llegar a la BD
- No depende únicamente de la BD para la integridad

## Configuración Spring Boot

### application-local.properties
```properties
spring.sql.init.data-locations=classpath:schema-sqlite.sql
spring.sql.init.mode=always
```

### application-test.properties
```properties
spring.sql.init.data-locations=classpath:schema-sqlite.sql
spring.sql.init.mode=always
```

**Cómo funciona:**
1. Hibernate ejecuta `create-drop` (crea tablas con constraints)
2. Spring Boot ejecuta `schema-sqlite.sql` automáticamente
3. Los índices UNIQUE se crean para reforzar la integridad

## Matriz de Protección

| Mecanismo | Tabla | Campo(s) | Costo | Beneficio |
|-----------|-------|---------|-------|----------|
| DB Constraint | Todos | Todos | Bajo | Evita duplicados a nivel BD |
| UNIQUE Index | Todos | Todos | Bajo | Refuerza constraint + mejora búsquedas |
| Regular Index | FK + común | Varios | Bajo | Mejora performance |
| Service Validation | Críticos | username, email, role name, permission code, module name | Medio | Mensajes claros de error |
| Application Logic | Critical | Validaciones complejas | Alto | Reglas de negocio |

## Archivos Modificados

✅ **Domain Entities:**
- `Users.java` - Agregado `@UniqueConstraint` para username, email
- `Role.java` - Agregado `@UniqueConstraint` para name
- `Permissions.java` - Agregado `@UniqueConstraint` para code
- `Module.java` - Agregado `@UniqueConstraint` para mainId, name
- `RolePermissions.java` - Agregado `@UniqueConstraint` compuesto
- `UserPermissions.java` - Agregado `@UniqueConstraint` compuesto

✅ **Configuration:**
- `SQLiteDialect.java` - Configurado para manejar constraints en CREATE TABLE
- `application-local.properties` - Agregado script de inicialización
- `application.properties` (test) - Agregado script de inicialización

✅ **Database Schema:**
- `schema-sqlite.sql` - Nuevo archivo con UNIQUE indexes y regular indexes

✅ **Services:**
- `UserService.java` - Validación de username y email
- `RoleService.java` - Nuevo servicio con validación
- `PermissionService.java` - Nuevo servicio con validación
- `ModuleCreationService.java` - Nuevo servicio con validación

## Verificación

✅ Todos los tests pasan (4/4)
✅ No hay errores de constraint en startup
✅ La base de datos tiene integridad referencial
✅ Las búsquedas tienen índices para performance
✅ Validación dual: BD + Aplicación

## Ventajas de Esta Solución

1. **Correcta para SQLite**: Usa características soportadas por SQLite
2. **Integridad BD**: Constraints en CREATE TABLE + UNIQUE indexes
3. **Performance**: Índices en foreign keys y búsquedas comunes
4. **Resiliencia**: Validación en application layer como respaldo
5. **Mantenibilidad**: Las constraints están definidas en JPA annotations
6. **Escalabilidad**: Fácil de migrar a otra BD relacional si es necesario

## Nota de Migración Futura

Si en el futuro se migra de SQLite a PostgreSQL, MySQL, etc.:
- Los `@UniqueConstraint` funcionarán sin cambios
- Los UNIQUE indexes se mantienen para performance
- Solo es necesario actualizar `spring.jpa.database-platform`
- No se necesitan cambios en el código de las entidades
