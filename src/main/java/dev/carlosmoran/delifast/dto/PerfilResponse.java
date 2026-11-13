package dev.carlosmoran.delifast.dto;

import dev.carlosmoran.delifast.domain.Rol;
import java.util.UUID;

/** Perfil del usuario autenticado (GET /me). */
public record PerfilResponse(
        UUID id,
        String nombre,
        String email,
        String telefono,
        Rol rol,
        Long negocioId,
        boolean esDueno) {
}
