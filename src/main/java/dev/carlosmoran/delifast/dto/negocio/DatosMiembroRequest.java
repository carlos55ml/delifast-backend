package dev.carlosmoran.delifast.dto.negocio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Nombre y teléfono de una persona del equipo. El email no se cambia (es el de su cuenta). */
public record DatosMiembroRequest(
        @NotBlank @Size(max = 100) String nombre,
        @Size(max = 20) String telefono) {
}
