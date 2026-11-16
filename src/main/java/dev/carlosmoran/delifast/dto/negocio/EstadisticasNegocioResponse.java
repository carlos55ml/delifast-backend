package dev.carlosmoran.delifast.dto.negocio;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Estadísticas del negocio (N06, RF-22), con el periodo anterior para comparar. */
public record EstadisticasNegocioResponse(
        Periodo periodo,
        LocalDate desde,
        LocalDate hasta,
        long pedidos,
        long pedidosAnterior,
        BigDecimal facturacion,
        BigDecimal facturacionAnterior,
        Integer tiempoMedioEntregaMin,
        Integer tiempoMedioEntregaAnteriorMin,
        Incidencias incidencias,
        List<Tramo> serie,
        List<ProductoVendido> masVendidos) {

    @Schema(name = "EstadisticasNegocioIncidencias")

    public record Incidencias(long total, long resueltas, long abiertas) {
    }

    @Schema(name = "EstadisticasNegocioTramo")

    public record Tramo(String etiqueta, long pedidos) {
    }

    @Schema(name = "EstadisticasNegocioProductoVendido")

    public record ProductoVendido(String nombre, long unidades) {
    }
}
