package dev.carlosmoran.delifast.dto.cliente;

import dev.carlosmoran.delifast.domain.EstadoPedido;
import dev.carlosmoran.delifast.domain.MetodoPago;
import dev.carlosmoran.delifast.domain.Pedido;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** Pedido tal como lo ve el cliente (RF-09, RF-10). */
public record PedidoClienteResponse(
        Long id,
        String codigo,
        NegocioRef negocio,
        EstadoPedido estado,
        MetodoPago metodoPago,
        String nota,
        String direccionEntrega,
        BigDecimal subtotal,
        BigDecimal gastosEnvio,
        BigDecimal total,
        Integer tiempoEstimadoMin,
        String motivoRechazo,
        OffsetDateTime fechaCreacion,
        List<Linea> lineas,
        List<Cambio> historial) {

    public record NegocioRef(Long id, String nombre) {
    }

    public record Linea(Long productoId, String nombreProducto, BigDecimal precioUnitario, int cantidad) {
    }

    public record Cambio(EstadoPedido estado, OffsetDateTime fecha) {
    }

    public static PedidoClienteResponse de(Pedido p) {
        return new PedidoClienteResponse(p.getId(), p.getCodigo(),
                new NegocioRef(p.getNegocio().getId(), p.getNegocio().getNombre()), p.getEstado(), p.getMetodoPago(),
                p.getNota(), p.getDireccionEntrega(), p.getSubtotal(), p.getGastosEnvio(), p.getTotal(),
                p.getTiempoEstimadoMin(), p.getMotivoRechazo(), p.getFechaCreacion(),
                p.getLineas().stream()
                        .map(l -> new Linea(l.getProducto() == null ? null : l.getProducto().getId(),
                                l.getNombreProducto(), l.getPrecioUnitario(), l.getCantidad()))
                        .toList(),
                p.getHistorial().stream().map(c -> new Cambio(c.getEstadoNuevo(), c.getFecha())).toList());
    }
}
