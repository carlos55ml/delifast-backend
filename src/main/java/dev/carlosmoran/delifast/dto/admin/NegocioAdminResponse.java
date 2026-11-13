package dev.carlosmoran.delifast.dto.admin;

import dev.carlosmoran.delifast.dto.LocalidadResponse;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Negocio en la lista de administración (A02). dueno es null si aún no tiene cuenta. */
public record NegocioAdminResponse(
        Long id,
        String nombre,
        LocalidadResponse localidad,
        String direccion,
        String telefono,
        Dueno dueno,
        long pedidosHoy,
        boolean abierto,
        boolean activo,
        OffsetDateTime fechaAlta) {

    public record Dueno(UUID id, String nombre, String email) {
    }
}
