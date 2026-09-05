# Guía de integración frontend

## Base URL y formato de respuesta

En local, la API se expone en:

```text
http://localhost:8080/api/v1
```

Salvo `POST /auth/logout`, las respuestas usan este sobre:

```json
{
  "data": {},
  "isSuccess": true,
  "message": "Mensaje descriptivo",
  "success": true
}
```

`isSuccess` y `success` representan el mismo estado. Ante un error, `data` es `null` y el status HTTP indica la causa (`400`, `401`, `403`, `404` o `409`).

## Flujo obligatorio al iniciar la aplicación

1. Iniciar sesión con `POST /auth/login`. No requiere token.
2. Guardar `data.token` de la respuesta.
3. Consultar `POST /modules/loadByUser` con el email del usuario para construir el menú y conocer las acciones disponibles. Tampoco requiere token.
4. Para cada endpoint protegido, enviar el token y los headers de módulo/acción correspondientes.

### 1. Login

```bash
curl --request POST \
  --url http://localhost:8080/api/v1/auth/login \
  --header 'Content-Type: application/json' \
  --data '{
    "email": "admin@local",
    "password": "admin123"
  }'
```

```json
{
  "data": {
    "token": "<JWT_TOKEN>",
    "type": "Bearer",
    "role": "ADMIN"
  },
  "isSuccess": true,
  "message": "Login successful",
  "success": true
}
```

Usuarios locales sembrados:

| Email | Password | Rol |
|---|---|---|
| `admin@local` | `admin123` | ADMIN |
| `owner@local` | `owner123` | OWNER |
| `user1@local` | `user123` | USER |

### 2. Cargar módulos y permisos del usuario

```bash
curl --request POST \
  --url http://localhost:8080/api/v1/modules/loadByUser \
  --header 'Content-Type: application/json' \
  --data '{
    "email": "admin@local"
  }'
```

No requiere `Authorization`, `X-Action` ni `X-Module-Main-Id`.

```json
{
  "data": [
    {
      "id": 2,
      "mainId": "MOD_RECEIPTS",
      "name": "Receipts",
      "parentId": null,
      "children": [],
      "permissions": {
        "READ": true,
        "WRITE": true
      }
    }
  ],
  "isSuccess": true,
  "message": "Modules loaded successfully",
  "success": true
}
```

El frontend debe mostrar un módulo si tiene alguna acción habilitada y habilitar altas, modificaciones y bajas solamente si `permissions.WRITE` es `true`.

## Headers de endpoints protegidos

Todos los endpoints, excepto `POST /auth/login` y `POST /modules/loadByUser`, requieren:

```http
Authorization: Bearer <JWT_TOKEN>
X-Action: READ | WRITE
X-Module-Main-Id: MOD_...
```

También pueden enviarse como query parameters: `?action=READ&main_id=MOD_RECEIPTS`. Se recomienda enviar headers.

| Recurso | `X-Module-Main-Id` | Para consultas | Para altas, edición y baja |
|---|---|---|---|
| Usuarios | `MOD_USERS` | `X-Action: READ` | `X-Action: WRITE` |
| Recepciones | `MOD_RECEIPTS` | `X-Action: READ` | `X-Action: WRITE` |
| Artículos | `MOD_ARTICLES` | `X-Action: READ` | `X-Action: WRITE` |
| Reparaciones | `MOD_REPAIRS` | `X-Action: READ` | `X-Action: WRITE` |
| Depósitos | `MOD_WAREHOUSES` | `X-Action: READ` | `X-Action: WRITE` |

Ejemplo de consulta protegida:

```bash
curl --request GET \
  --url http://localhost:8080/api/v1/receipts/findAll \
  --header 'Authorization: Bearer <JWT_TOKEN>' \
  --header 'X-Action: READ' \
  --header 'X-Module-Main-Id: MOD_RECEIPTS'
```

## Matriz de permisos del seed local

Los permisos por usuario pueden sobrescribir esta matriz. La respuesta de `loadByUser` siempre es la fuente de verdad para la interfaz.

| Módulo | USER | ADMIN | OWNER |
|---|---|---|---|
| Usuarios | Sin acceso | READ y WRITE | READ y WRITE |
| Recepciones | READ | READ y WRITE | READ |
| Artículos | Sin acceso | READ y WRITE | READ y WRITE |
| Reparaciones | READ y WRITE | READ y WRITE | READ y WRITE |
| Depósitos | READ | READ | READ y WRITE |

## Restricciones por depósito

Además del permiso de módulo, las operaciones sobre receipts, repairs y warehouses se limitan por los depósitos asociados al usuario:

| Rol | Recepciones | Reparaciones | Depósitos |
|---|---|---|---|
| OWNER | Ve todas las recepciones. | Ve y opera en todos los depósitos. | Ve todos y es el único que puede crear, editar o eliminar. |
| ADMIN | Sólo ve/edita recepciones cuyo **origen y destino** estén en sus depósitos asociados. | Sólo opera y ve reparaciones de sus depósitos asociados. | Sólo ve sus depósitos asociados. |
| USER | Sólo ve recepciones cuyo **destino** sea su único depósito asociado. | Sólo opera y ve reparaciones de su único depósito asociado. No ve historial. | Sólo ve su único depósito asociado. |

En la respuesta de una recepción, `purchasePrice` sólo se envía para ADMIN y OWNER. USER recibe `supplier`, `salePrice` y `totalAmount`, pero nunca `purchasePrice`.

## Endpoints de autenticación

| Método y ruta | Uso | Protección |
|---|---|---|
| `POST /auth/login` | Valida email y contraseña; devuelve JWT. | Pública |
| `POST /auth/token` | Crea o recupera un token de sesión. No usar como login de frontend. | Requiere token y headers de autorización |
| `POST /auth/logout` | Invalida el bearer actual y la sesión. | Requiere bearer |

Logout:

```bash
curl --request POST \
  --url http://localhost:8080/api/v1/auth/logout \
  --header 'Authorization: Bearer <JWT_TOKEN>' \
  --header 'X-Action: NONE'
```

Respuesta: `204 No Content`.

## Endpoints de módulos

| Método y ruta | Uso | Protección |
|---|---|---|
| `POST /modules/loadByUser` | Obtiene árbol de módulos y permisos efectivos a partir del email. | Pública |
| `POST /modules/refreshViewed` | Recarga un módulo puntual y sus hijos si el usuario conserva acceso. | Bearer + READ del módulo solicitado |

Recargar el módulo Receipts:

```bash
curl --request POST \
  --url http://localhost:8080/api/v1/modules/refreshViewed \
  --header 'Authorization: Bearer <JWT_TOKEN>' \
  --header 'X-Action: READ' \
  --header 'X-Module-Main-Id: MOD_RECEIPTS' \
  --header 'Content-Type: application/json' \
  --data '{
    "email": "admin@local",
    "moduleMainId": "MOD_RECEIPTS"
  }'
```

## Endpoints de usuarios

| Método y ruta | Uso | Acción |
|---|---|---|
| `GET /users/find` | Lista usuarios. | `READ`, `MOD_USERS` |
| `POST /users/register` | Crea un usuario. | `WRITE`, `MOD_USERS` |
| `PUT /users/edit` | Modifica email, nickname o contraseña. | `WRITE`, `MOD_USERS` |
| `DELETE /users/delete` | Elimina un usuario por email. | `WRITE`, `MOD_USERS` |
| `PUT /users/editRole` | Cambia el rol de un usuario. | `WRITE`, `MOD_USERS` |
| `PUT /users/editWarehouses` | Reemplaza los depósitos asociados. | `WRITE`, `MOD_USERS` |
| `PUT /users/assignWarehouses` | Alias de `editWarehouses`. | `WRITE`, `MOD_USERS` |

Crear un USER asociado a un único depósito:

```bash
curl --request POST \
  --url http://localhost:8080/api/v1/users/register \
  --header 'Authorization: Bearer <JWT_TOKEN>' \
  --header 'X-Action: WRITE' \
  --header 'X-Module-Main-Id: MOD_USERS' \
  --header 'Content-Type: application/json' \
  --data '{
    "nickName": "operador.sur",
    "email": "operador.sur@example.com",
    "password": "cambiar-esta-clave",
    "roleName": "USER",
    "warehouseIds": [1]
  }'
```

```json
{
  "data": {
    "id": 4,
    "nickName": "operador.sur",
    "email": "operador.sur@example.com",
    "roleId": 1,
    "roleName": "USER",
    "warehouseIds": [1],
    "warehouseCodes": ["WH-001"]
  },
  "isSuccess": true,
  "message": "User registered successfully",
  "success": true
}
```

Asociar depósitos a un usuario:

```bash
curl --request PUT \
  --url http://localhost:8080/api/v1/users/editWarehouses \
  --header 'Authorization: Bearer <JWT_TOKEN>' \
  --header 'X-Action: WRITE' \
  --header 'X-Module-Main-Id: MOD_USERS' \
  --header 'Content-Type: application/json' \
  --data '{
    "email": "operador.sur@example.com",
    "warehouseIds": [1]
  }'
```

OWNER puede asignar depósitos a ADMIN o USER. ADMIN sólo puede asignar depósitos propios a usuarios USER. USER debe tener exactamente un depósito; OWNER no tiene depósitos asociados.

## Endpoints de artículos

| Método y ruta | Uso | Acción |
|---|---|---|
| `GET /articles/article/{isExternalSku}` | Busca por SKU (`false`) o SKU externo (`true`). El criterio va en el body. | `READ`, `MOD_ARTICLES` |
| `POST /articles/register` | Crea un artículo. | `WRITE`, `MOD_ARTICLES` |
| `PUT /articles/edit/{articleId}` | Actualiza campos provistos del artículo. | `WRITE`, `MOD_ARTICLES` |
| `DELETE /articles/delete/{articleId}` | Elimina el artículo si no está en receipts abiertas. | `WRITE`, `MOD_ARTICLES` |

Crear artículo:

```bash
curl --request POST \
  --url http://localhost:8080/api/v1/articles/register \
  --header 'Authorization: Bearer <JWT_TOKEN>' \
  --header 'X-Action: WRITE' \
  --header 'X-Module-Main-Id: MOD_ARTICLES' \
  --header 'Content-Type: application/json' \
  --data '{
    "sku": "ART-0100",
    "externalSku": "EXT-100",
    "name": "Cubierta urbana",
    "type": "SPARE",
    "supplier": "Proveedor SA",
    "purchasePrice": 35.00,
    "salePrice": 49.90
  }'
```

```json
{
  "data": {
    "id": 4,
    "sku": "ART-0100",
    "externalSku": "EXT-100",
    "name": "Cubierta urbana",
    "type": "SPARE",
    "supplier": "Proveedor SA",
    "purchasePrice": 35.00,
    "salePrice": 49.90
  },
  "isSuccess": true,
  "message": "Article created successfully",
  "success": true
}
```

Buscar por SKU:

```bash
curl --request GET \
  --url http://localhost:8080/api/v1/articles/article/false \
  --header 'Authorization: Bearer <JWT_TOKEN>' \
  --header 'X-Action: READ' \
  --header 'X-Module-Main-Id: MOD_ARTICLES' \
  --header 'Content-Type: application/json' \
  --data '{"sku":"ART-0100"}'
```

## Endpoints de depósitos

| Método y ruta | Uso | Acción |
|---|---|---|
| `GET /warehouses/findAll` | Lista depósitos visibles para el usuario. | `READ`, `MOD_WAREHOUSES` |
| `GET /warehouses/find/{warehouseId}` | Obtiene un depósito visible. | `READ`, `MOD_WAREHOUSES` |
| `POST /warehouses/register` | Crea depósito. Sólo OWNER. | `WRITE`, `MOD_WAREHOUSES` |
| `PUT /warehouses/edit/{warehouseId}` | Edita depósito. Sólo OWNER. | `WRITE`, `MOD_WAREHOUSES` |
| `DELETE /warehouses/delete/{warehouseId}` | Elimina depósito. Sólo OWNER. | `WRITE`, `MOD_WAREHOUSES` |

Listar depósitos:

```bash
curl --request GET \
  --url http://localhost:8080/api/v1/warehouses/findAll \
  --header 'Authorization: Bearer <JWT_TOKEN>' \
  --header 'X-Action: READ' \
  --header 'X-Module-Main-Id: MOD_WAREHOUSES'
```

```json
{
  "data": [
    {
      "id": 1,
      "code": "WH-001",
      "name": "Berazategui",
      "active": true,
      "creationDate": "2026-09-05T16:00:00",
      "editDate": "2026-09-05T16:00:00"
    }
  ],
  "isSuccess": true,
  "message": "Warehouses retrieved successfully",
  "success": true
}
```

Crear depósito:

```bash
curl --request POST \
  --url http://localhost:8080/api/v1/warehouses/register \
  --header 'Authorization: Bearer <OWNER_JWT_TOKEN>' \
  --header 'X-Action: WRITE' \
  --header 'X-Module-Main-Id: MOD_WAREHOUSES' \
  --header 'Content-Type: application/json' \
  --data '{
    "code": "WH-004",
    "name": "Quilmes",
    "active": true
  }'
```

## Endpoints de recepciones

| Método y ruta | Uso | Acción |
|---|---|---|
| `GET /receipts/findAll` | Lista receipts filtradas por rol y depósitos asociados. | `READ`, `MOD_RECEIPTS` |
| `GET /receipts/findByUserAndWarehouse?userId={id}&warehouseCode={code}` | Lista receipts de un usuario que involucren el depósito. El resultado también se filtra por el solicitante. | `READ`, `MOD_RECEIPTS` |
| `GET /receipts/history/{receiptId}` | Historial de cambios de estado. | `READ`, `MOD_RECEIPTS` |
| `POST /receipts/register` | Crea receipt y detalles de artículos. | `WRITE`, `MOD_RECEIPTS` |
| `PUT /receipts/edit/{receiptId}` | Cambia estado y agrega detalles; no elimina detalles existentes. | `WRITE`, `MOD_RECEIPTS` |
| `DELETE /receipts/delete/{receiptId}` | Elimina la receipt y registra su historial. | `WRITE`, `MOD_RECEIPTS` |

Crear receipt:

```bash
curl --request POST \
  --url http://localhost:8080/api/v1/receipts/register \
  --header 'Authorization: Bearer <JWT_TOKEN>' \
  --header 'X-Action: WRITE' \
  --header 'X-Module-Main-Id: MOD_RECEIPTS' \
  --header 'Content-Type: application/json' \
  --data '{
    "userId": 1,
    "status": 10,
    "origin": "WH-001",
    "destiny": "WH-002",
    "description": "Transferencia inicial",
    "details": [
      { "articleId": 1 },
      { "articleId": 2 }
    ]
  }'
```

`receiptKey` es opcional: si no se envía, la API lo genera.

Respuesta de una receipt para ADMIN u OWNER:

```json
{
  "data": {
    "id": 1,
    "receiptKey": "RPCAAAA0000001",
    "status": 10,
    "statusDescription": "new",
    "origin": "WH-001",
    "destiny": "WH-002",
    "description": "Transferencia inicial",
    "userId": 1,
    "username": "admin",
    "userEmail": "admin@local",
    "totalAmount": 133.90,
    "details": [
      {
        "id": 1,
        "articleId": 1,
        "articleSku": "ART-0001",
        "articleName": "Mountain Tire",
        "supplier": "Contoso",
        "salePrice": 49.90,
        "purchasePrice": 35.00,
        "creationDate": "2026-09-05T16:00:00",
        "editDate": null
      },
      {
        "id": 2,
        "articleId": 2,
        "articleSku": "ART-0002",
        "articleName": "Hydraulic Brake Kit",
        "supplier": "Fabrikam",
        "salePrice": 84.00,
        "purchasePrice": 58.00,
        "creationDate": "2026-09-05T16:00:00",
        "editDate": null
      }
    ]
  },
  "isSuccess": true,
  "message": "Receipt created successfully",
  "success": true
}
```

Para USER, cada objeto de `details` no contiene la propiedad `purchasePrice`.

Actualizar el estado y agregar artículo:

```bash
curl --request PUT \
  --url http://localhost:8080/api/v1/receipts/edit/1 \
  --header 'Authorization: Bearer <JWT_TOKEN>' \
  --header 'X-Action: WRITE' \
  --header 'X-Module-Main-Id: MOD_RECEIPTS' \
  --header 'Content-Type: application/json' \
  --data '{
    "status": 55,
    "details": [
      { "articleId": 3 }
    ]
  }'
```

## Endpoints de reparaciones

| Método y ruta | Uso | Acción |
|---|---|---|
| `GET /repairs/findAll` | Lista reparaciones dentro del alcance de depósitos. | `READ`, `MOD_REPAIRS` |
| `GET /repairs/findByWarehouse?warehouseCode={code}` | Lista reparaciones de un depósito al que el usuario tiene acceso. | `READ`, `MOD_REPAIRS` |
| `GET /repairs/history/{repairId}` | Lista la auditoría de una reparación. USER no puede consultarlo. | `READ`, `MOD_REPAIRS` |
| `POST /repairs/register` | Crea reparación en un depósito accesible. | `WRITE`, `MOD_REPAIRS` |
| `PUT /repairs/edit/{repairId}` | Actualiza reparación y artículos asociados. | `WRITE`, `MOD_REPAIRS` |
| `DELETE /repairs/delete/{repairId}` | Elimina reparación y registra auditoría. | `WRITE`, `MOD_REPAIRS` |

Crear reparación:

```bash
curl --request POST \
  --url http://localhost:8080/api/v1/repairs/register \
  --header 'Authorization: Bearer <JWT_TOKEN>' \
  --header 'X-Action: WRITE' \
  --header 'X-Module-Main-Id: MOD_REPAIRS' \
  --header 'Content-Type: application/json' \
  --data '{
    "warehouseId": 1,
    "articleIds": [1, 3],
    "price": 120.00,
    "description": "Cambio de cubierta y manubrio"
  }'
```

```json
{
  "data": {
    "id": 1,
    "warehouseId": 1,
    "warehouseCode": "WH-001",
    "warehouseName": "Berazategui",
    "price": 120.00,
    "description": "Cambio de cubierta y manubrio",
    "articles": [
      { "id": 1, "sku": "ART-0001", "name": "Mountain Tire" },
      { "id": 3, "sku": "ART-0003", "name": "Carbon Handlebar" }
    ],
    "userId": 1,
    "username": "admin",
    "userEmail": "admin@local",
    "creationDate": "2026-09-05T16:00:00",
    "editDate": null
  },
  "isSuccess": true,
  "message": "Repair created successfully",
  "success": true
}
```

## Errores que debe manejar el frontend

| Status | Significado | Acción recomendada |
|---:|---|---|
| `400` | Body inválido, headers de acción/módulo faltantes o regla de negocio incumplida. | Mostrar `message` y corregir el request. |
| `401` | Falta bearer, es inválido o expiró. | Borrar sesión local y redirigir a login. |
| `403` | El usuario no tiene permiso de módulo o está fuera de su alcance de depósitos. | Ocultar/inhabilitar la operación y mostrar `message`. |
| `404` | Recurso, usuario o módulo inexistente. | Actualizar la pantalla o volver al listado. |
| `409` | Email o nickname duplicado. | Pedir otro valor. |

## Ejemplo de cliente HTTP

El token debe ir en cada llamada protegida. La acción debe coincidir con lo que se realizará.

```js
const API_URL = "http://localhost:8080/api/v1";

async function apiRequest(path, { token, moduleMainId, action, ...options } = {}) {
  const headers = {
    "Content-Type": "application/json",
    ...(options.headers ?? {})
  };

  if (token) {
    headers.Authorization = `Bearer ${token}`;
    headers["X-Action"] = action;
    headers["X-Module-Main-Id"] = moduleMainId;
  }

  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    headers,
    credentials: "include"
  });

  if (response.status === 204) {
    return null;
  }

  const body = await response.json();
  if (!response.ok) {
    throw new Error(body.message);
  }
  return body;
}

const login = await apiRequest("/auth/login", {
  method: "POST",
  body: JSON.stringify({ email: "admin@local", password: "admin123" })
});

const token = login.data.token;

const modules = await apiRequest("/modules/loadByUser", {
  method: "POST",
  body: JSON.stringify({ email: "admin@local" })
});

const receipts = await apiRequest("/receipts/findAll", {
  token,
  moduleMainId: "MOD_RECEIPTS",
  action: "READ"
});
```
