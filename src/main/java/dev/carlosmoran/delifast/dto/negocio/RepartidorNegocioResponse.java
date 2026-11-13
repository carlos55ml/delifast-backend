package dev.carlosmoran.delifast.dto.negocio;

import java.util.UUID;

/** Repartidor en el selector de asignación (N08). */
public record RepartidorNegocioResponse(
        UUID id,
        String nombre,
        String email,
        String telefono,
        boolean activo,
        boolean disponible,
        long pedidosEnReparto) {
}
