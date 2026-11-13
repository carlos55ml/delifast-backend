# Contrato de la API

`openapi.yaml` (OpenAPI 3.1) es el contrato REST entre la API y sus clientes. El original vive en
`DeliFast_Web/docs/api/openapi.yaml`, que genera sus tipos desde él; esta es una copia que hay que
mantener igual.

Ahora cubre la zona de administración (`/admin/**`, RF-28..RF-31), `/me` y `/localidades`. Al
implementar los controladores:

- Los DTO deben coincidir con los esquemas (nombres en camelCase, fechas ISO 8601).
- Todo `/admin/**` exige el rol `ADMINISTRADOR` (claim `user_rol` del JWT de Supabase).
- Los errores son `ProblemDetail` (ya lo hace `GlobalExceptionHandler`); los 400 llevan `errores`
  con el formato `campo: mensaje`, que la web coloca junto a cada campo del formulario.
- Falta añadir el 409 (email ya registrado) al `GlobalExceptionHandler`.
- Para comprobar que la API cumple el contrato, se puede comparar con el que genera springdoc
  (`/v3/api-docs`).
