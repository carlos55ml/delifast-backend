package dev.carlosmoran.delifast.dto.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Alta de un negocio con la cuenta de su dueño (POST /admin/negocios). */
public record AltaNegocioRequest(
        @NotBlank @Size(max = 100) String nombre,
        @NotNull Long localidadId,
        @NotBlank @Size(max = 200) String direccion,
        @NotBlank @Size(min = 9, max = 20) String telefono,
        @NotNull @Valid Dueno dueno) {

    public record Dueno(
            @NotBlank @Size(max = 100) String nombre,
            @NotBlank @Email @Size(max = 254) String email) {
    }

    public DatosNegocioRequest datos() {
        return new DatosNegocioRequest(nombre, localidadId, direccion, telefono);
    }
}
