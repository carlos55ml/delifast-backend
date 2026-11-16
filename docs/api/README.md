# Documentación de la API

Hay dos descripciones OpenAPI 3.1 de la misma API:

| Fichero | Qué es | Quién lo escribe |
| --- | --- | --- |
| `openapi.yaml` | **Contrato** acordado con la web y la app: rutas, esquemas y errores esperados. Es la fuente de verdad. | A mano (hilos de la web y del backend) |
| `generado/openapi.json` y `generado/openapi.yaml` | Documentación **generada por springdoc** a partir de los controladores y DTO del código. | `OpenApiGeneradoTest` |
| `generado/swagger.html` | Swagger UI con la documentación generada dentro. Se abre con doble clic, sin arrancar la API ni la base de datos. | `OpenApiGeneradoTest` |

Cubre administración (`/admin/**`), panel del negocio (`/negocio/**` y las acciones sobre
`/pedidos/{id}/...` e `/incidencias/{id}/...`), vista del repartidor (`/repartidor/**`,
`/repartos/**`), app del cliente (`/negocios`, `/me/direcciones`, `/pedidos`), `/me` y `/localidades`.

## Ver la documentación

- **Sin arrancar nada:** abre `docs/api/generado/swagger.html` en el navegador (carga Swagger UI
  desde unpkg, así que necesita Internet).
- **Con la API en marcha** (`./mvnw spring-boot:run`, perfil `dev`):
  - Swagger UI: <http://localhost:8080/swagger-ui.html>. En el desplegable de arriba se elige entre
    *Implementada (springdoc)* y *Contrato (docs/api/openapi.yaml)*.
  - JSON: <http://localhost:8080/v3/api-docs> · YAML: <http://localhost:8080/v3/api-docs.yaml>
  - Para probar endpoints protegidos: botón **Authorize** y pegar el access token de Supabase
    (sin `Bearer`). Se recuerda al recargar (`persist-authorization`).
- En el perfil `prod` Swagger UI y `/v3/api-docs` están apagados.

## Regenerar

```bash
./mvnw test -Dtest=OpenApiGeneradoTest
```

El test arranca la aplicación completa **sin PostgreSQL** (`SinBaseDeDatos`: excluye DataSource,
JPA y Flyway y sustituye los repositorios por mocks), pide `/v3/api-docs` y reescribe
`docs/api/generado/`. Corre también con `./mvnw test`, así que basta con hacer commit de los
cambios de esa carpeta junto con los del código.

Además **comprueba el contrato**: falla si el código no implementa exactamente las mismas rutas,
métodos y `operationId` que `openapi.yaml`, o si la versión de `OpenApiConfig.VERSION` no es la
del contrato. Cada controlador lleva `@Operation(operationId, summary, tags)` copiados del contrato.

## Reglas al implementar

- Los DTO deben coincidir con los esquemas del contrato (camelCase, fechas ISO 8601, importes con
  2 decimales). Los records anidados llevan `@Schema(name = ...)` para que sus nombres no choquen
  en la documentación generada.
- Roles (claim `user_rol` del JWT de Supabase): `/admin/**` → `ADMINISTRADOR`; `/negocio/**` y
  acciones sobre pedidos e incidencias → `NEGOCIO`, siempre sobre su `negocio_id`;
  `/repartidor/**` y `/repartos/**` → `REPARTIDOR`; `/me/direcciones` y `/pedidos` (crear, listar,
  ver) → `CLIENTE`. Públicos: `/localidades`, `/negocios`, `/negocios/{id}/carta`.
- Errores en `ProblemDetail`: los 400 llevan `errores` con el formato `campo: mensaje`. 401 y 403
  los escribe `ProblemDetailSecurityHandler`; el resto, `GlobalExceptionHandler`.
- El contrato lo editan dos hilos: antes de cambiarlo, `git pull` y partir de la copia de este repo.
