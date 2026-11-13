package dev.carlosmoran.delifast.dto.negocio;

import dev.carlosmoran.delifast.dto.LocalidadResponse;

/** Cabecera del panel del negocio (GET /negocio). */
public record MiNegocioResponse(
        Long id,
        String nombre,
        LocalidadResponse localidad,
        boolean abierto,
        boolean abiertoManual,
        long pedidosHoy) {
}
