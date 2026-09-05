# Ejemplos de salida para frontend

Este directorio incluye ejemplos de respuesta de la API para maquetado y consumo desde el frontend.

Convenciones:
- La mayoría de endpoints responden con el envelope `ResponseData`:
  ```json
  {
    "data": { },
    "isSuccess": true,
    "message": "Texto descriptivo"
  }
  ```
- `/auth/token` devuelve directamente un objeto `TokenResponse`.
- `/auth/logout` responde con `204 No Content`.
- Los IDs, fechas y textos son ejemplos representativos siguiendo la seed local del proyecto.

Estructura:
- `auth/`: login y token
- `modules/`: permisos del usuario
- `users/`: listado y mantenimiento de usuarios
- `articles/`: gestión de artículos
- `warehouses/`: gestión de depósitos
- `receipts/`: documentos de ingreso/traslado
- `repairs/`: reparaciones

> Las respuestas pueden variar según el usuario autenticado, su rol y los warehouses asociados.

## 1) Endpoint para armar el menú del frontend

La app debe cargar el menú lateral (o horizontal) usando este endpoint:

- Método: `POST`
- Ruta: `/modules/loadByUser`
- Body:

```json
{
  "email": "admin@local"
}
```

Respuesta esperada:

```json
{
  "data": [
    {
      "id": 1,
      "mainId": "MOD_USERS",
      "name": "Users",
      "parentId": null,
      "children": [],
      "permissions": {
        "READ": true,
        "WRITE": true
      }
    },
    {
      "id": 2,
      "mainId": "MOD_WAREHOUSES",
      "name": "Warehouses",
      "parentId": null,
      "children": [],
      "permissions": {
        "READ": true,
        "WRITE": false
      }
    }
  ],
  "isSuccess": true,
  "message": "Modules loaded successfully"
}
```

Ejemplo completo en archivo:
- `examples/examples-frontend/modules/loadByUser.json`

## 2) Qué datos recibe el frontend

Ese endpoint devuelve una estructura jerárquica de módulos. Cada nodo tiene:

- `id`: id interno del módulo
- `mainId`: identificador de negocio del módulo, ejemplo: `MOD_USERS`, `MOD_ARTICLES`, `MOD_RECEIPTS`
- `name`: nombre a mostrar en el menú
- `parentId`: id del módulo padre; si es `null`, es un módulo raíz
- `children`: array de submódulos
- `permissions`: map con permisos calculados para ese módulo, por ejemplo:
  - `READ`: `true`/`false`
  - `WRITE`: `true`/`false`

Importante: el backend ya resuelve permisos con rol + overrides de usuario. El frontend no debería inventar permisos: solo debe leer lo que llega en `permissions`.

## 3) Regla para pintar el menú

Regla principal:
- Un módulo se pinta si tiene al menos un permiso activo (`true`) en `permissions`.
- Si un módulo raíz tiene `children`, se debe mostrar como agrupador.
- Si un módulo tiene `children`, puede renderizarse como menú desplegable o como sección con subitems.
- Si todos los valores de `permissions` vienen en `false` o vacíos, no se debe mostrar ese nodo.

Pseudo-regla:

```ts
const visibleModules = modules.filter((module) => {
  const hasPermission = Object.values(module.permissions ?? {}).some(Boolean);
  return hasPermission;
});
```

## 4) Regla para menú lateral (sidebar)

Para un sidebar:
- Tomar los módulos raíz: `parentId === null`
- Recorrer `children` para armar jerarquía
- Renderizar cada raíz como item principal
- Renderizar `children` como subitems bajo ese módulo

Ejemplo visual:

```text
Users
Warehouses
  ├─ Warehouse list
  ├─ Create warehouse
Articles
Receipts
Repairs
```

## 5) Regla para menú horizontal / top nav

Si la UI usa menú horizontal, la regla es la misma, pero en vez de volcar toda la jerarquía a la izquierda, se toman los módulos raíz como tabs principales y los `children` como dropdown o submenú:

```ts
const rootModules = response.data.filter((m) => m.parentId === null);

const topNav = rootModules.map((module) => ({
  key: module.mainId,
  label: module.name,
  children: module.children ?? []
}));
```

Regla práctica:
- El primer nivel del menú es siempre `parentId === null`
- Los `children` son la segunda capa
- `mainId` es la clave única para navegación
- `name` es el texto visible
- `permissions` define si puede mostrarse o no

## 6) Recomendaciones de implementación

- No usar rutas hardcodeadas del backend en el frontend. Usar `mainId` para mapear acción y navegación.
- Si un módulo tiene `children`, no hace falta crear una ruta aparte si el padre ya representa la sección.
- Para la navegación, el frontend puede usar `mainId` como identificador y `name` como texto visible.
- Si `permissions.READ` es `false` y `permissions.WRITE` es `false`, ese modulo debe ocultarse.

## 7) Ejemplo de salida real del endpoint

El archivo más importante es:
- `modules/loadByUser.json`

Allí se ve el formato real de la respuesta que el frontend debe consumir para generar el menú.

## 8) Uso recomendado del flujo completo

1. Loguearse con `/auth/login`
2. Guardar el token recibido
3. Llamar a `/modules/loadByUser` con el email del usuario
4. Filtrar los módulos visibles por permiso
5. Construir el árbol para el sidebar o top menu
6. Usar `mainId` para navegar a cada sección de la app

Ejemplo de request:

```json
{
  "email": "owner@local"
}
```

Ejemplo de respuesta:

```json
{
  "data": [
    {
      "id": 3,
      "mainId": "MOD_ARTICLES",
      "name": "Articles",
      "parentId": null,
      "children": [],
      "permissions": {
        "READ": true,
        "WRITE": true
      }
    }
  ],
  "isSuccess": true,
  "message": "Modules loaded successfully"
}
```

> El frontend debe interpretar `data` como un árbol de módulos, no como una lista plana. La jerarquía está dada por `parentId` y `children`.
