package dev.carlosmoran.delifast.dto.repartidor;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/** Perfil del repartidor con su disponibilidad y actividad de hoy (R06). */
public record PerfilRepartidorResponse(
        UUID id,
        String nombre,
        String email,
        String telefono,
        NegocioRef negocio,
        boolean disponible,
        long repartosHoy,
        long enCurso) {

    @Schema(name = "PerfilRepartidorNegocioRef")

    public record NegocioRef(Long id, String nombre, String localidad) {
    }
}
