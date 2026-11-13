package dev.carlosmoran.delifast.dto.negocio;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Alta de un empleado o repartidor por el dueño. */
public record AltaMiembroRequest(
        @NotBlank @Size(max = 100) String nombre,
        @Size(max = 20) String telefono,
        @NotBlank @Email @Size(max = 254) String email) {
}
