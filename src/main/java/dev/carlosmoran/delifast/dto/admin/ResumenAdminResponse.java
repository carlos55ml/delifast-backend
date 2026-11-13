package dev.carlosmoran.delifast.dto.admin;

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

    public record PedidosNegocioHoy(Long negocioId, String nombre, String localidad, long pedidos, boolean abierto) {
    }
}
