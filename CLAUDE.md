# DeliFast Backend · instrucciones para Claude Code

API REST Spring Boot 4.1 (Java 21) de DeliFast. Paquete base `dev.carlosmoran.delifast`.
Specs de referencia del proyecto: arquitectura, modelo de clases y modelo E-R.

## Base de datos (Supabase `delifast-dev`, MCP `supabase` en `.mcp.json`)

- El esquema `public` lo gestiona **solo Flyway**: ficheros `src/main/resources/db/migration/V<n>__<descripcion>.sql`,
  que se aplican al arrancar la API. Una migración aplicada no se edita; los cambios van en una nueva.
- Con el MCP de Supabase **no** se usa `apply_migration` ni se ejecuta DDL con `execute_sql`, y no se crean
  migraciones de Supabase CLI (`supabase/migrations`): tendrían un historial distinto del de Flyway.
- El MCP sí se usa para consultar datos y el esquema, revisar logs y advisors de seguridad/rendimiento y leer la documentación.
- No se tocan los esquemas `auth`, `storage` ni `realtime` salvo desde migraciones Flyway (FK a `auth.users`, hook del token, RLS, políticas de Storage).
- Hibernate en modo `validate`: las entidades JPA deben coincidir con las migraciones.

## Convenciones

- Capas: `controller` (DTO validados) → `service` (reglas de negocio) → `repository` (Spring Data JPA) → `domain` (entidades).
- Nunca exponer entidades en la API: siempre DTO.
- Credenciales solo en `.env` (no se sube a git). La clave `service_role` nunca sale del backend.
- Comandos: `./mvnw spring-boot:run`, `./mvnw test`.
