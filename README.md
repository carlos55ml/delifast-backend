# DeliFast Backend

API REST de **DeliFast**, la plataforma de pedidos y reparto para pequeños negocios de hostelería.
Contiene la lógica de negocio (pedidos, carta, repartos, incidencias, estadísticas y administración)
que usan la app Android/iOS y la web.

## Tecnologías

- Java 21 y Spring Boot 4.1 (Web MVC, Data JPA, Validation, Security + OAuth2 Resource Server, Actuator)
- **Supabase** (PostgreSQL, Auth, Realtime y Storage) en la nube: proyecto `delifast-dev` para desarrollo y `delifast-prod` para producción
- Migraciones del esquema con Flyway
- springdoc-openapi (Swagger UI)
- Maven (con Maven Wrapper, no hace falta instalarlo) y Docker para empaquetar la API

## Requisitos

- JDK 21
- IntelliJ IDEA (recomendado)
- Acceso al proyecto Supabase `delifast-dev`
- Docker, solo si se quiere ejecutar la API en un contenedor

## Puesta en marcha (desarrollo)

```bash
# 1. Credenciales del proyecto delifast-dev
cp .env.example .env
#    Rellena DB_URL, DB_USER y DB_PASSWORD con los datos del panel de Supabase:
#    botón "Connect" → "Session pooler" (la conexión directa es solo IPv6).

# 2. API (perfil dev por defecto). Al arrancar, Flyway aplica las migraciones pendientes.
./mvnw spring-boot:run
```

Spring Boot lee el `.env` automáticamente (`spring.config.import`). En IntelliJ basta con ejecutar `DeliFastApplication`.

| Servicio | URL |
|---|---|
| API | http://localhost:8080 |
| Estado | http://localhost:8080/actuator/health |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Base de datos | Panel de Supabase → *Table Editor* / *SQL Editor* (o cualquier cliente con los datos del Session pooler) |

Otros comandos útiles:

```bash
./mvnw test                     # tests (usan el .env de delifast-dev)
./mvnw package                  # genera target/delifast-backend-*.jar
docker compose up -d --build    # API en un contenedor con la configuración de .env
docker compose logs -f api      # logs de la API en el contenedor
docker compose down             # parar el contenedor
```

## Perfiles y entornos

| Perfil | Proyecto Supabase | Credenciales | Particularidades |
|---|---|---|---|
| `dev` (por defecto) | `delifast-dev` | `.env` | SQL en consola, logs DEBUG, Swagger |
| `prod` | `delifast-prod` | Variables de entorno de Render/Railway | Swagger desactivado |

## Estructura

```
src/main/java/dev/carlosmoran/delifast/
├── config/       seguridad, CORS y OpenAPI
├── security/     conversión del JWT de Supabase a usuario + rol
├── controller/   endpoints REST, uno por recurso
├── dto/          objetos de entrada y salida de la API
├── service/      reglas de negocio
├── repository/   interfaces Spring Data JPA
├── domain/       entidades JPA (clases del diagrama de clases)
└── exception/    errores de negocio y manejador global
src/main/resources/
├── application.yml        configuración común
├── application-dev.yml    perfil de desarrollo
├── application-prod.yml   perfil de producción
└── db/migration/          migraciones Flyway (V1__..., V2__...)
```

Flujo de una petición: **Controller** (valida el DTO) → **Service** (aplica la regla) → **Repository** (PostgreSQL) → JSON.

## Base de datos

- El esquema `public` lo crea y versiona **Flyway** al arrancar la API, con los ficheros de `src/main/resources/db/migration/`. Las mismas migraciones se aplican en `delifast-dev` y en `delifast-prod`.
- Los esquemas propios de Supabase (`auth`, `storage`, `realtime`) no se tocan.
- No se cambia el esquema desde el panel de Supabase ni con Supabase CLI: todo cambio es una migración nueva.
- Hibernate arranca en modo `validate`: no crea tablas, solo comprueba que las entidades coinciden con la BD.
- La clave `service_role` y la contraseña de la BD solo viven en el backend (`.env` o variables de entorno), nunca en las apps.

## Estado

Estructura inicial. Pendiente: migración `V1` con las tablas del modelo E-R, entidades JPA,
integración con Supabase Auth (validación del JWT y roles) y los primeros endpoints.
Plan gratuito de Supabase: el proyecto se pausa tras 7 días sin uso (se reactiva desde el panel).

## Documentación

- [docs/estructura.md](docs/estructura.md): estructura de carpetas, capas del código y solución de errores de dependencias.

## Autor

Carlos Morán · Proyecto final del CFGS Desarrollo de Aplicaciones Multiplataforma.
