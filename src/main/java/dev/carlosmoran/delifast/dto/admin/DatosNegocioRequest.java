package dev.carlosmoran.delifast.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Datos básicos de un negocio que corrige el administrador (PUT /admin/negocios/{id}). */
public record DatosNegocioRequest(
        @NotBlank @Size(max = 100) String nombre,
        @NotNull Long localidadId,
        @NotBlank @Size(max = 200) String direccion,
        @NotBlank @Size(min = 9, max = 20) String telefono) {
}
