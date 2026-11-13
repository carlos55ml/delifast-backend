# Contrato de la API

`openapi.yaml` (OpenAPI 3.1) es el contrato REST entre la API y sus clientes. El original vive en
`DeliFast_Web/docs/api/openapi.yaml`, que genera sus tipos desde él; esta es una copia que hay que
mantener igual.

Ahora cubre la zona de administración (`/admin/**`, RF-28..RF-31), los pedidos del negocio
(`/negocio/**` y `/pedidos/{id}/aceptar|rechazar|listo|asignar`), `/me` y `/localidades`. Al
implementar los controladores:

- Los DTO deben coincidir con los esquemas (nombres en camelCase, fechas ISO 8601).
- Todo `/admin/**` exige el rol `ADMINISTRADOR` (claim `user_rol` del JWT de Supabase), y
  `/negocio/**` y las acciones sobre pedidos el rol `NEGOCIO`, siempre sobre su `negocio_id`.
- Los errores son `ProblemDetail` (ya lo hace `GlobalExceptionHandler`); los 400 llevan `errores`
  con el formato `campo: mensaje`, que la web coloca junto a cada campo del formulario.
- 401 y 403 los escribe `ProblemDetailSecurityHandler` (filtros de seguridad); 409 y 502 (fallo de
  Supabase Auth) el `GlobalExceptionHandler`.
- Para comprobar que la API cumple el contrato, se puede comparar con el que genera springdoc
  (`/v3/api-docs`).
