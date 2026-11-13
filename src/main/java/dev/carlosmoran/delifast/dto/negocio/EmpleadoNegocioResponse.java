package dev.carlosmoran.delifast.dto.negocio;

import dev.carlosmoran.delifast.domain.GestorNegocio;
import java.util.UUID;

/** Persona del negocio: dueño o empleado (N04). */
public record EmpleadoNegocioResponse(UUID id, String nombre, String email, String telefono, boolean esDueno,
                                      boolean activo) {

    public static EmpleadoNegocioResponse de(GestorNegocio g) {
        return new EmpleadoNegocioResponse(g.getId(), g.getNombre(), g.getEmail(), g.getTelefono(), g.isEsDueno(),
                g.isActivo());
    }
}
