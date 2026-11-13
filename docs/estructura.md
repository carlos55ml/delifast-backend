# DeliFast Backend · Estructura del proyecto

Actualizado el 1-11-2026.

## Vista general

Es la estructura estándar de Maven, la misma en casi cualquier proyecto Spring Boot: el código va en `src/main/java`, la configuración en `src/main/resources`, los tests en `src/test` y el resto son ficheros de la raíz.

```
DeliFast_Backend/
├── pom.xml                  qué librerías usa el proyecto y con qué versión
├── mvnw, mvnw.cmd, .mvn/    Maven Wrapper: descarga Maven solo, no hace falta instalarlo
├── .env.example             plantilla de credenciales (copiar a .env, que no se sube a git)
├── Dockerfile, docker-compose.yml   empaquetar la API en un contenedor
├── CLAUDE.md, .mcp.json, .claude/   instrucciones y herramientas para Claude Code
├── docs/                    documentación del backend (este fichero)
└── src/
    ├── main/
    │   ├── java/dev/carlosmoran/delifast/
    │   │   ├── DeliFastApplication.java   punto de entrada (main)
    │   │   ├── config/       seguridad, CORS, Swagger
    │   │   ├── security/     leer el JWT de Supabase (por hacer)
    │   │   ├── controller/   endpoints REST
    │   │   ├── dto/          datos que entran y salen por la API
    │   │   ├── service/      reglas de negocio
    │   │   ├── repository/   acceso a la base de datos
    │   │   ├── domain/       entidades JPA (las tablas) y enumeraciones
    │   │   └── exception/    errores con mensaje claro
    │   └── resources/
    │       ├── application.yml, application-dev.yml, application-prod.yml
    │       └── db/migration/V1__esquema_inicial.sql
    └── test/java/dev/carlosmoran/delifast/   tests
```

La carpeta `target/` aparece al compilar: es generada, no se toca ni se sube a git.

## src/main/java: el código por capas

La ruta `dev/carlosmoran/delifast` no es una carpeta "de verdad" que elijas tú: es el **paquete** Java `dev.carlosmoran.delifast`, y Java exige que cada punto del paquete sea una carpeta. Por eso hay tres niveles antes de llegar al código.

Dentro, cada subpaquete es una **capa**, y cada capa solo habla con la de debajo:

| Paquete | Qué contiene | Ejemplo en DeliFast |
| --- | --- | --- |
| `controller` | Clases con `@RestController`: reciben la petición HTTP y devuelven JSON. No deciden nada. | `PedidoController` con `POST /pedidos` |
| `dto` | Objetos simples con lo que entra y sale por la API, validados con `@NotNull`, `@Size`… | `CrearPedidoRequest`, `PedidoResponse` |
| `service` | Las reglas del negocio, con `@Service` y `@Transactional`. | `PedidoService`: negocio abierto, pedido mínimo, recalcular el total |
| `repository` | Interfaces de Spring Data JPA; Spring escribe las consultas por ti. | `PedidoRepository extends JpaRepository<Pedido, Long>` |
| `domain` | Entidades JPA: una clase por tabla (ya están las 12) y los `enum`. | `Pedido`, `Reparto`, `EstadoPedido` |
| `config` | Configuración de Spring. | `SecurityConfig`, `CorsConfig`, `OpenApiConfig` |
| `security` | Convertir el JWT de Supabase en usuario + rol (todavía vacío). | |
| `exception` | Errores de negocio y el manejador que los convierte en respuestas JSON. | `ReglaNegocioException` → 422 |

Recorrido de una petición, por ejemplo crear un pedido:

1. La app llama a `POST /pedidos` con un JSON → `PedidoController` lo convierte en un DTO y lo valida.
2. El controller llama a `PedidoService.crear(dto)`, que comprueba las reglas y construye un `Pedido` (entidad de `domain`).
3. El service lo guarda con `PedidoRepository.save(pedido)` → Hibernate genera el `INSERT` en PostgreSQL.
4. El service devuelve el pedido, el controller lo convierte en `PedidoResponse` y Spring lo envía como JSON.

Si algo falla en el paso 2 (por ejemplo, pedido por debajo del mínimo), el service lanza `ReglaNegocioException` y `GlobalExceptionHandler` responde un 422 con el mensaje. Las entidades nunca salen por la API: siempre se convierten a DTO.

## src/main/resources: configuración y migraciones

Todo lo que no es código Java pero va dentro de la aplicación.

- `application.yml`: configuración común. Lee las credenciales de `.env` (`DB_URL`, `DB_USER`, `DB_PASSWORD`), pone Hibernate en modo `validate` y configura Flyway.
- `application-dev.yml` y `application-prod.yml`: lo que cambia según el **perfil**. En `dev` se ven las SQL en consola; en `prod` se apaga Swagger. El perfil por defecto es `dev`.
- `db/migration/`: las migraciones de Flyway. Al arrancar, Flyway aplica en orden las que falten (`V1__esquema_inicial.sql`, luego `V2__…`) y las apunta en `flyway.flyway_schema_history`. Una migración ya aplicada no se edita: cualquier cambio va en una nueva.

Orden al arrancar la API: Spring lee la configuración → Flyway aplica las migraciones pendientes → Hibernate comprueba que las entidades de `domain` coinciden con las tablas → la API empieza a escuchar en el puerto 8080.

## Ficheros de la raíz

| Fichero | Para qué sirve |
| --- | --- |
| `pom.xml` | Define el proyecto para Maven: Spring Boot 4.1.2 como padre y las dependencias (*starters*). Maven las descarga en `~/.m2`. |
| `mvnw`, `mvnw.cmd`, `.mvn/` | Maven Wrapper. `./mvnw spring-boot:run` arranca la API y `./mvnw test` pasa los tests sin instalar Maven. |
| `.env.example` → `.env` | Credenciales de Supabase. Copia la plantilla a `.env` y rellénala; `.env` está en `.gitignore`. |
| `Dockerfile`, `docker-compose.yml` | Empaquetar la API en un contenedor para desplegarla (Render o Railway). En el día a día no hacen falta. |
| `CLAUDE.md`, `.mcp.json`, `.claude/` | Reglas y herramientas para Claude Code (por ejemplo, cambiar el esquema solo con Flyway). |
| `docs/` | Documentación del backend, como este fichero. |
| `.idea/` | Configuración de IntelliJ. Se genera sola. |
| `.gitignore`, `.gitattributes`, `.editorconfig`, `.dockerignore` | Qué no sube a git ni a Docker, finales de línea y estilo del editor. |

## Errores de dependencias en IntelliJ

Si IntelliJ marca dependencias en rojo después de cambiar el `pom.xml` (por ejemplo, al subir de versión de Spring Boot), casi siempre es porque no ha recargado Maven. Prueba en este orden:

1. **Recargar Maven**: en la pestaña *Maven* (derecha), botón *Reload All Maven Projects*, o el icono flotante *Load Maven Changes* que sale al abrir el `pom.xml`. (Así se arregló el 1-11-2026.)
2. **Forzar la descarga**: en la terminal del proyecto, `./mvnw -U clean compile`. Si esto funciona y IntelliJ sigue en rojo, el problema es solo de IntelliJ.
3. **Comprobar el JDK**: *File → Project Structure → SDK* tiene que ser Java 21 o superior. En *Settings → Build Tools → Maven → Runner* también tiene que ser un JDK 21.
4. **Modo offline**: en la pestaña *Maven*, el icono de *Toggle Offline Mode* tiene que estar desactivado.
5. **Índices viejos**: *File → Invalidate Caches… → Invalidate and Restart*.

Si al arrancar aparece `Could not resolve placeholder 'DB_URL'`, no es un problema de dependencias: falta el `.env`. Copia `.env.example` a `.env` y rellénalo con los datos del *Session pooler* de `delifast-dev`.
