package dev.carlosmoran.delifast.dto.negocio;

import io.swagger.v3.oas.annotations.media.Schema;
import dev.carlosmoran.delifast.domain.CambioEstado;
import dev.carlosmoran.delifast.domain.EstadoPedido;
import dev.carlosmoran.delifast.domain.LineaPedido;
import dev.carlosmoran.delifast.domain.MetodoPago;
import dev.carlosmoran.delifast.domain.Pedido;
import dev.carlosmoran.delifast.domain.Reparto;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Pedido tal como lo ve el negocio: tablero (N01) y detalle con historial (N02). */
public record PedidoNegocioResponse(
        Long id,
        String codigo,
        EstadoPedido estado,
        OffsetDateTime fechaCreacion,
        Cliente cliente,
        String direccionEntrega,
        String telefonoContacto,
        MetodoPago metodoPago,
        String nota,
        List<Linea> lineas,
        BigDecimal subtotal,
        BigDecimal gastosEnvio,
        BigDecimal total,
        Integer tiempoEstimadoMin,
        String motivoRechazo,
        RepartoPedido reparto,
        List<Cambio> historial) {

    @Schema(name = "PedidoNegocioCliente")

    public record Cliente(String nombre) {
    }

    @Schema(name = "PedidoNegocioLinea")

    public record Linea(String nombreProducto, BigDecimal precioUnitario, int cantidad, BigDecimal importe) {
    }

    @Schema(name = "PedidoNegocioCambio")

    public record Cambio(EstadoPedido estadoAnterior, EstadoPedido estadoNuevo, OffsetDateTime fecha, String autor) {
    }

    @Schema(name = "PedidoNegocioRepartoPedido")

    public record RepartoPedido(Long id, Repartidor repartidor, OffsetDateTime fechaAsignacion,
                                OffsetDateTime fechaEntrega) {
    }

    @Schema(name = "PedidoNegocioRepartidor")

    public record Repartidor(UUID id, String nombre) {
    }

    public static PedidoNegocioResponse de(Pedido p) {
        Reparto r = p.getReparto();
        return new PedidoNegocioResponse(p.getId(), p.getCodigo(), p.getEstado(), p.getFechaCreacion(),
                new Cliente(p.getCliente().getNombre()), p.getDireccionEntrega(), p.getTelefonoContacto(),
                p.getMetodoPago(), p.getNota(),
                p.getLineas().stream().map(PedidoNegocioResponse::linea).toList(),
                p.getSubtotal(), p.getGastosEnvio(), p.getTotal(), p.getTiempoEstimadoMin(), p.getMotivoRechazo(),
                r == null ? null : new RepartoPedido(r.getId(),
                        new Repartidor(r.getRepartidor().getId(), r.getRepartidor().getNombre()),
                        r.getFechaAsignacion(), r.getFechaEntrega()),
                p.getHistorial().stream().map(PedidoNegocioResponse::cambio).toList());
    }

    private static Linea linea(LineaPedido l) {
        return new Linea(l.getNombreProducto(), l.getPrecioUnitario(), l.getCantidad(), l.getImporte());
    }

    private static Cambio cambio(CambioEstado c) {
        return new Cambio(c.getEstadoAnterior(), c.getEstadoNuevo(), c.getFecha(),
                c.getAutor() == null ? null : c.getAutor().getNombre());
    }
}
