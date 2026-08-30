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
| admin@local     | admin123   | ADMIN | WH-001, WH-002      |
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
| 10     | new                  | Recibo nuevo                 |
| 55     | on preparation       |                              |
| 75     | ready to dispatched  |                              |
| 95     | dispatched           |                              |
| 110    | receipt              | Confirmado recibido          |

> **Nota**: El estado 0 (`deleted`) es interno del sistema y no puede usarse al crear recibos.

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
s  "http://localhost:8080/repairs/findAll?action=READ&main_id=MOD_REPAIRS"
```

## Autorización centralizada (interceptor)

- `BearerTokenInterceptor` protege todas las rutas excepto `POST /auth/login`.
- El interceptor extrae `userId`, `roleId`, `roleName` del token.
- El frontend debe enviar en cada request protegido:
  - `action`: `READ` | `WRITE` | `NONE`
  - `main_id`: obligatorio para `READ` y `WRITE` (ej: `MOD_ARTICLES`, `MOD_RECEIPTS`, `MOD_REPAIRS`, `MOD_USERS`, `MOD_WAREHOUSES`)
- Alternativa equivalente (menos visible en URL):
  - Header `X-Action`
  - Header `X-Module-Main-Id`
- Reglas aplicadas:
  - `NONE`: solo valida token.
  - `READ`: valida permisos con `role_permissions` y override en `user_permissions`.
  - `WRITE`: requiere `ADMIN` o `OWNER` + permiso `WRITE` (con override de usuario).

Ejemplo `WRITE`:

```bash
curl -X POST "http://localhost:8080/articles/register?action=WRITE&main_id=MOD_ARTICLES" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"sku":"ART-0100","name":"Disc Brake Rotor","type":"SPARE","supplier":"Contoso","purchasePrice":44.50,"salePrice":69.90}'
```

Ejemplo equivalente con headers:

```bash
curl -X POST "http://localhost:8080/articles/register" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "X-Action: WRITE" \
  -H "X-Module-Main-Id: MOD_ARTICLES" \
  -H "Content-Type: application/json" \
  -d '{"sku":"ART-0100","name":"Disc Brake Rotor","type":"SPARE","supplier":"Contoso","purchasePrice":44.50,"salePrice":69.90}'
```

## Política de Roles y Warehouses

| Rol   | Warehouses (READ) | Warehouses (WRITE CRUD) | Receipts `findAll` | Editar warehouses de usuarios |
|-------|--------------------|--------------------------|--------------------|-------------------------------|
| OWNER | Todos              | ✅                        | Todas              | ✅ (ADMIN y USER)             |
| ADMIN | Solo asociados     | ❌                        | Solo receipts con `origin` y `destiny` dentro de sus warehouses asociados | ✅ (solo usuarios con rol USER y solo warehouses asociados al ADMIN) |
| USER  | Solo su warehouse  | ❌                        | Solo receipts con `destiny` = su warehouse asignado | ❌ |

> Para endpoints de warehouses usa `main_id=MOD_WAREHOUSES`.

Para endpoints de repairs (`main_id=MOD_REPAIRS`):
- `USER` y `ADMIN` solo pueden crear/editar/eliminar/listar repairs en warehouses asociados.
- `OWNER` puede operar en todas las repairs.
- `USER` nunca puede consultar historial de repairs.

## Endpoints de Usuarios

| Método | Ruta                  | Query params requeridos                    | Descripción                              |
|--------|-----------------------|--------------------------------------------|------------------------------------------|
| GET    | /users/find           | `action=READ&main_id=MOD_USERS`            | Listar todos los usuarios                |
| POST   | /users/register       | `action=WRITE&main_id=MOD_USERS`           | Crear usuario                            |
| PUT    | /users/edit           | `action=WRITE&main_id=MOD_USERS`           | Editar perfil (email, nickname, password)|
| PUT    | /users/editRole       | `action=WRITE&main_id=MOD_USERS`           | Cambiar rol de usuario                   |
| PUT    | /users/editWarehouses | `action=WRITE&main_id=MOD_USERS`           | Configurar warehouses de un usuario (ADMIN solo USER y dentro de su alcance; OWNER sin restricción por rol) |
| PUT    | /users/assignWarehouses | `action=WRITE&main_id=MOD_USERS`         | Alias explícito para asociación de warehouses a usuarios USER/ADMIN |
| DELETE | /users/delete         | `action=WRITE&main_id=MOD_USERS`           | Eliminar usuario                         |

## Endpoints de Artículos

| Método | Ruta                    | Query params requeridos                    | Descripción         |
|--------|-------------------------|--------------------------------------------|---------------------|
| POST   | /articles/register      | `action=WRITE&main_id=MOD_ARTICLES`        | Crear artículo      |
| PUT    | /articles/edit/{id}     | `action=WRITE&main_id=MOD_ARTICLES`        | Editar artículo     |
| DELETE | /articles/delete/{id}   | `action=WRITE&main_id=MOD_ARTICLES`        | Eliminar artículo (hard delete con validación de recibos) |

> Un artículo solo se elimina si no está referenciado por recibos con estado menor a `receipt` (110).

## Endpoints de Recibos

| Método | Ruta                          | Query params requeridos                    | Descripción                                       |
|--------|-------------------------------|--------------------------------------------|---------------------------------------------------|
| POST   | /receipts/register            | `action=WRITE&main_id=MOD_RECEIPTS`        | Crear recibo                                      |
| PUT    | /receipts/edit/{id}           | `action=WRITE&main_id=MOD_RECEIPTS`        | Editar recibo + loguea cambio de estado si cambia |
| DELETE | /receipts/delete/{id}         | `action=WRITE&main_id=MOD_RECEIPTS`        | Eliminar recibo → registra snapshot en `receipt_status_log` con status=0 |
| GET    | /receipts/findAll             | `action=READ&main_id=MOD_RECEIPTS`         | Listar recibos con alcance por rol (USER/ADMIN/OWNER) |
| GET    | /receipts/findByUserAndWarehouse | `action=READ&main_id=MOD_RECEIPTS`      | Listar por usuario y warehouse                    |
| GET    | /receipts/history/{id}        | `action=READ&main_id=MOD_RECEIPTS`         | Historial de cambios de estado (`receipt_status_log`) |

### Auditoría de Recibos

- **Creación**: se registra snapshot inicial en `receipt_status_log` (`previousStatus=null`, `newStatus=status inicial`).
- **Cambio de estado**: cada modificación que cambia el status se registra en `receipt_status_log`.
- **Eliminación**: se registra snapshot final en `receipt_status_log` con `newStatus=0 (deleted)`, `user_id` y `user_email` del solicitante.

## Endpoints de Warehouses

| Método | Ruta                         | Query params requeridos                       | Descripción |
|--------|------------------------------|-----------------------------------------------|-------------|
| POST   | /warehouses/register         | `action=WRITE&main_id=MOD_WAREHOUSES`         | Crear warehouse (solo OWNER) |
| PUT    | /warehouses/edit/{id}        | `action=WRITE&main_id=MOD_WAREHOUSES`         | Editar warehouse (solo OWNER) |
| DELETE | /warehouses/delete/{id}      | `action=WRITE&main_id=MOD_WAREHOUSES`         | Eliminar warehouse (solo OWNER) |
| GET    | /warehouses/findAll          | `action=READ&main_id=MOD_WAREHOUSES`          | Listar warehouses visibles según rol |
| GET    | /warehouses/find/{id}        | `action=READ&main_id=MOD_WAREHOUSES`          | Obtener warehouse por id dentro de alcance |

## Endpoints de Reparaciones

| Método | Ruta                    | Query params requeridos                    | Descripción                                        |
|--------|-------------------------|--------------------------------------------|----------------------------------------------------|
| POST   | /repairs/register       | `action=WRITE&main_id=MOD_REPAIRS`         | Crear reparación (sin estado)                      |
| PUT    | /repairs/edit/{id}      | `action=WRITE&main_id=MOD_REPAIRS`         | Editar reparación (sin estado)                     |
| DELETE | /repairs/delete/{id}    | `action=WRITE&main_id=MOD_REPAIRS`         | Eliminar reparación + snapshot en `repair_audit`   |
| GET    | /repairs/findAll        | `action=READ&main_id=MOD_REPAIRS`          | Listar reparaciones filtradas por alcance de warehouses |
| GET    | /repairs/findByWarehouse | `action=READ&main_id=MOD_REPAIRS`         | Listar reparaciones por warehouse dentro del alcance del rol |
| GET    | /repairs/history/{id}   | `action=READ&main_id=MOD_REPAIRS`          | Historial de auditoría (`USER` no permitido)       |

### Auditoría de Reparaciones

Cada operación (CREATE, UPDATE, DELETE) genera un registro en la tabla `repair_audit` con:
- `repair_id`, `price`, `warehouse_id`, `article_id`, `user_id`
- `action_type`: `CREATE` | `UPDATE` | `DELETE`
- `description`: snapshot textual (descripción vigente o "Repair deleted")
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
│   ├── create-receipt.json        POST /receipts/register
│   ├── edit-receipt-path-variable.json PUT /receipts/edit/{id}
│   ├── edit-receipt-body.json     PUT /receipts/edit/{id}
│   ├── find-by-user-and-warehouse.json GET /receipts/findByUserAndWarehouse
│   ├── get-all-receipts.json      GET /receipts/findAll
│   ├── owner-history-receipt.json GET /receipts/history/{id}  (OWNER)
│   └── delete-receipt-path-variable.json DELETE /receipts/delete/{id}
├── warehouses/
│   ├── register-warehouse.json    POST /warehouses/register
│   ├── edit-warehouse-path-variable.json PUT /warehouses/edit/{id}
│   ├── edit-warehouse-body.json   PUT /warehouses/edit/{id}
│   ├── delete-warehouse-path-variable.json DELETE /warehouses/delete/{id}
│   ├── find-all-warehouses.json   GET /warehouses/findAll
│   └── find-warehouse-by-id-path-variable.json GET /warehouses/find/{id}
├── repairs/
│   ├── register-repair.json       POST /repairs/register
│   ├── edit-repair.json           PUT  /repairs/edit/{id}
│   ├── delete-repair.json         DELETE /repairs/delete/{id}
│   ├── find-all-repairs.json      GET  /repairs/findAll
│   ├── find-by-warehouse-repairs.json GET /repairs/findByWarehouse
│   └── repair-history.json        GET  /repairs/history/{id}  (USER no permitido)
└── users/
    └── (ejemplos de usuario)
```

## Seguridad

Este mecanismo de token es intencionalmente mínimo y ligado a la sesión. No es OAuth2/JWT y no es adecuado tal cual para sistemas en producción expuestos a Internet.
