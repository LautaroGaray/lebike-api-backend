# Spring Boot + Maven + SQLite scaffold

API scaffold con:

- Java 10 source/target
- Spring Boot 2.1.18.RELEASE
- Maven-compatible project
- SQLite por defecto
- Perfil PostgreSQL de ejemplo para cambio a SQL server convencional
- JPA/Hibernate code-first schema (`ddl-auto=update`)
- Abstracción genérica `IRepository<T, ID>`
- Bearer token generado por `/auth/token`
- Lifetime del token ligado al `HttpSession` del servlet

## Nota importante sobre la versión de Java

No existe una release estándar de OpenJDK llamada `10.0.12`. El proyecto está configurado para Java nivel 10.

Spring Boot 3.x no se usa intencionalmente porque requiere Java 17 o superior.

## Requisitos

- JDK 10
- Maven 3.9.x

```bash
java -version
mvn -version
```

## Ejecutar con SQLite

```bash
mvn spring-boot:run
```

Perfiles:
- `application.properties` → `spring.profiles.active=local` (por defecto)
- `application-local.properties` → SQLite datasource
- `application-dev.properties` → SQL datasource (Railway/PostgreSQL)
- `application-prod.properties` → SQL datasource (producción)

Para perfil `dev`:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

La base de datos SQLite se crea en:
```text
${user.home}/springboot-sqlite-scaffold-local.db
```

## Usuarios de seed (perfil `local`)

| Email           | Password   | Rol   | Warehouses          |
|-----------------|------------|-------|---------------------|
| owner@local     | owner123   | OWNER | todos               |
| admin@local     | admin123   | ADMIN | todos (sin restricción) |
| user1@local     | user123    | USER  | WH-001 (Berazategui)|

## Warehouses de seed

| Código  | Nombre       |
|---------|--------------|
| WH-001  | Berazategui  |
| WH-002  | Ezpeleta     |
| WH-003  | Bernal       |

## Artículos de seed

| SKU       | Nombre               | Tipo      | Proveedor |
|-----------|----------------------|-----------|-----------|
| ART-0001  | Mountain Tire        | SPARE     | Contoso   |
| ART-0002  | Hydraulic Brake Kit  | SPARE     | Fabrikam  |
| ART-0003  | Carbon Handlebar     | COMPONENT | Northwind |

## Catálogo de estados (Status)

| Código | Descripción          | Uso                          |
|--------|----------------------|------------------------------|
| 0      | deleted              | Sistema interno (auditoría)  |
| 10     | new                  | Recibo/Reparación nueva      |
| 55     | on preparation       |                              |
| 75     | ready to dispatched  |                              |
| 95     | dispatched           |                              |
| 110    | receipt              | Confirmado recibido          |

> **Nota**: El estado 0 (`deleted`) es interno del sistema y no puede usarse al crear recibos ni reparaciones.

## Autenticación

### 1. Generar token

```bash
curl -i -X POST http://localhost:8080/auth/token \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@local","password":"admin123"}'
```

Respuesta:
```json
{ "token": "...", "type": "Bearer" }
```

### 2. Llamar endpoint protegido

```bash
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/repairs/findAll
```

## Política de Roles y Warehouses

| Rol   | Warehouses visibles                                | Receipts WRITE | Repairs WRITE |
|-------|----------------------------------------------------|----------------|---------------|
| OWNER | Todos                                              | ❌ (READ only)  | ✅             |
| ADMIN | Todos, salvo que OWNER asigne lista específica     | ✅              | ✅             |
| USER  | Exactamente 1 warehouse asignado                  | ❌ (READ only)  | ✅             |

> El OWNER puede configurar los warehouses de un ADMIN mediante `PUT /users/editWarehouses`.

## Endpoints de Usuarios

| Método | Ruta                  | Rol mínimo | Descripción                              |
|--------|-----------------------|------------|------------------------------------------|
| GET    | /users/find           | ADMIN      | Listar todos los usuarios                |
| POST   | /users/register       | ADMIN      | Crear usuario                            |
| PUT    | /users/edit           | ADMIN      | Editar perfil (email, nickname, password)|
| PUT    | /users/editRole       | OWNER      | Cambiar rol de usuario                   |
| PUT    | /users/editWarehouses | OWNER      | Configurar warehouses de un usuario      |
| DELETE | /users/delete         | OWNER      | Eliminar usuario                         |

## Endpoints de Artículos

| Método | Ruta                    | Rol           | Descripción         |
|--------|-------------------------|---------------|---------------------|
| POST   | /articles/register      | ADMIN/OWNER   | Crear artículo      |
| PUT    | /articles/edit/{id}     | ADMIN/OWNER   | Editar artículo     |
| DELETE | /articles/delete/{id}   | ADMIN/OWNER   | Eliminar (soft delete) artículo |

> La eliminación de artículos es lógica (soft delete) para preservar referencias históricas en recibos.

## Endpoints de Recibos

| Método | Ruta                          | Rol           | Descripción                                       |
|--------|-------------------------------|---------------|---------------------------------------------------|
| POST   | /receipts/register            | ADMIN (WRITE) | Crear recibo                                      |
| PUT    | /receipts/edit/{id}           | ADMIN (WRITE) | Editar recibo + loguea cambio de estado si cambia |
| DELETE | /receipts/delete/{id}         | ADMIN (WRITE) | Eliminar recibo → registra en `receipt_status_histpry` con status=0 |
| GET    | /receipts/findAll             | todos (READ)  | Listar (filtrado por warehouse según rol)         |
| GET    | /receipts/findByUserAndWarehouse | todos (READ)| Listar por usuario y warehouse                    |
| GET    | /receipts/history/{id}        | OWNER (READ)  | Historial de cambios de estado (`receipt_status_log`) |

### Auditoría de Recibos

- **Cambio de estado**: cada modificación que cambia el status se registra en `receipt_status_log`.
- **Eliminación**: al eliminar un recibo se registra en `receipt_status_histpry` con `status=0 (deleted)`, `user_id` y `user_email` del solicitante.

## Endpoints de Reparaciones

| Método | Ruta                    | Rol            | Descripción                                        |
|--------|-------------------------|----------------|----------------------------------------------------|
| POST   | /repairs/register       | todos (WRITE)  | Crear reparación (USER: solo su warehouse)         |
| PUT    | /repairs/edit/{id}      | todos (WRITE)  | Editar reparación (warehouse restriction aplicada) |
| DELETE | /repairs/delete/{id}    | todos (WRITE)  | Eliminar → registra en `repair_audit` con status=0 |
| GET    | /repairs/findAll        | todos (READ)   | Listar (filtrado por warehouse según rol)          |
| GET    | /repairs/history/{id}   | OWNER (READ)   | Historial de auditoría de la reparación            |

### Auditoría de Reparaciones

Cada operación (CREATE, UPDATE, DELETE) genera un registro en la tabla `repair_audit` con:
- `repair_id`, `price`, `warehouse_id`, `article_id`, `user_id`
- `status`: estado al momento de la acción (0 para DELETE)
- `action_type`: `CREATE` | `UPDATE` | `DELETE`
- `description`: descripción de la reparación o "Repair deleted"
- `creation_date`: timestamp del evento

## Patrón Repository

```java
public interface IRepository<T, ID> {
    T save(T entity);
    Optional<T> findById(ID id);
    List<T> findAll();
    void deleteById(ID id);
}
```

Las queries JPQL se cargan desde `repository-queries.properties`. No se usan derived query names de Spring Data para evitar problemas con nombres de campo.

## Ejemplos Bruno (dev/local)

Los archivos `.json` en `examples/dev-local/bruno/` **nunca deben pasar a producción**.

```
examples/dev-local/bruno/
├── articles/
│   ├── register-article.json      POST /articles/register
│   ├── edit-article.json          PUT  /articles/edit/{id}
│   └── delete-article.json        DELETE /articles/delete/{id}
├── receipts/
│   ├── owner-history-receipt.json GET /receipts/history/{id}  (OWNER)
│   └── delete-receipt-path-variable.json DELETE /receipts/delete/{id}
├── repairs/
│   ├── register-repair.json       POST /repairs/register
│   ├── edit-repair.json           PUT  /repairs/edit/{id}
│   ├── delete-repair.json         DELETE /repairs/delete/{id}
│   ├── find-all-repairs.json      GET  /repairs/findAll
│   └── repair-history.json        GET  /repairs/history/{id}  (OWNER)
└── users/
    └── (ejemplos de usuario)
```

## Seguridad

Este mecanismo de token es intencionalmente mínimo y ligado a la sesión. No es OAuth2/JWT y no es adecuado tal cual para sistemas en producción expuestos a Internet.
