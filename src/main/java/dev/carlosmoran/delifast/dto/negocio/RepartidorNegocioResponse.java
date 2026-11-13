package dev.carlosmoran.delifast.dto.negocio;

import java.util.UUID;

/** Repartidor del negocio (N04 y selector de asignación de N08). */
public record RepartidorNegocioResponse(
        UUID id,
        String nombre,
        String email,
        String telefono,
        boolean activo,
        boolean disponible,
        long pedidosEnReparto,
        long repartosHoy) {
}
