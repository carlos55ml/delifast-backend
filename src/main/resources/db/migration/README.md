# Migraciones Flyway

Ficheros SQL versionados que crean y modifican el esquema. Flyway los aplica
en orden al arrancar el backend, sobre el esquema `public` del proyecto Supabase
(`delifast-dev` en desarrollo, `delifast-prod` en producción), y guarda el historial en `flyway.flyway_schema_history` (esquema `flyway`, que la API de datos no expone).

- Las migraciones usan siempre nombres con esquema (`public.pedidos`), porque Flyway trabaja con `flyway` como esquema por defecto.

- Nombre: `V<numero>__<descripcion>.sql`, por ejemplo `V1__esquema_inicial.sql`.
- Una migración ya aplicada **no se edita**: los cambios van en una nueva (`V2__...`).
- El esquema sale de `specs/modelo-entidad-relacion.md` del proyecto.
- No se usa Supabase CLI ni el editor de tablas de Supabase para cambiar el esquema: todo cambio pasa por aquí.

## Paso manual tras la V1 (una vez por proyecto Supabase)

- *Authentication → Hooks → Customize Access Token*: elegir `public.custom_access_token_hook`.
- *Authentication → Sessions / JWT*: caducidad del token de unos 15 minutos (900 s).
