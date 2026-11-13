package dev.carlosmoran.delifast.dto.negocio;

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

    public record Incidencias(long total, long resueltas, long abiertas) {
    }

    public record Tramo(String etiqueta, long pedidos) {
    }

    public record ProductoVendido(String nombre, long unidades) {
    }
}
