package dev.carlosmoran.delifast.dto.repartidor;

import dev.carlosmoran.delifast.domain.EstadoPedido;
import dev.carlosmoran.delifast.domain.MetodoPago;
import dev.carlosmoran.delifast.domain.Pedido;
import dev.carlosmoran.delifast.domain.Reparto;
import dev.carlosmoran.delifast.dto.negocio.PedidoNegocioResponse;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** Reparto tal como lo ve su repartidor (R01, R02, R05). */
public record RepartoRepartidorResponse(
        Long id,
        EstadoPedido estado,
        OffsetDateTime fechaAsignacion,
        OffsetDateTime fechaEntrega,
        MetodoPago metodoCobro,
        PedidoRef pedido) {

    public record Cliente(String nombre) {
    }

    public record PedidoRef(Long id, String codigo, Cliente cliente, String direccionEntrega, Double latitud,
                            Double longitud, String telefonoContacto, MetodoPago metodoPago, String nota,
                            List<PedidoNegocioResponse.Linea> lineas, BigDecimal subtotal, BigDecimal gastosEnvio,
                            BigDecimal total) {
    }

    public static RepartoRepartidorResponse de(Reparto r) {
        Pedido p = r.getPedido();
        return new RepartoRepartidorResponse(r.getId(), p.getEstado(), r.getFechaAsignacion(), r.getFechaEntrega(),
                r.getMetodoCobro(),
                new PedidoRef(p.getId(), p.getCodigo(), new Cliente(p.getCliente().getNombre()), p.getDireccionEntrega(),
                        p.getLatitudEntrega(), p.getLongitudEntrega(), p.getTelefonoContacto(), p.getMetodoPago(),
                        p.getNota(),
                        p.getLineas().stream()
                                .map(l -> new PedidoNegocioResponse.Linea(l.getNombreProducto(), l.getPrecioUnitario(),
                                        l.getCantidad(), l.getImporte()))
                                .toList(),
                        p.getSubtotal(), p.getGastosEnvio(), p.getTotal()));
    }
}
