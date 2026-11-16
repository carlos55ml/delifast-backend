package dev.carlosmoran.delifast.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Resumen de la plataforma (A01). */
public record ResumenAdminResponse(
        long negociosActivos,
        long negociosTotales,
        long pedidosHoy,
        long localidadesConPedidosHoy,
        long incidenciasAbiertas,
        long incidenciasHoy,
        long repartidoresEnRuta,
        List<PedidosNegocioHoy> pedidosHoyPorNegocio,
        List<IncidenciaAdminResponse> incidenciasRecientes) {

    @Schema(name = "ResumenAdminPedidosNegocioHoy")

    public record PedidosNegocioHoy(Long negocioId, String nombre, String localidad, long pedidos, boolean abierto) {
    }
}
