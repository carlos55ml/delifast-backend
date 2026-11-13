package dev.carlosmoran.delifast.dto.negocio;

import dev.carlosmoran.delifast.domain.EstadoIncidencia;
import dev.carlosmoran.delifast.domain.Incidencia;
import dev.carlosmoran.delifast.domain.MetodoPago;
import dev.carlosmoran.delifast.domain.OrigenIncidencia;
import dev.carlosmoran.delifast.domain.Pedido;
import dev.carlosmoran.delifast.domain.TipoIncidencia;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Incidencia de un pedido tal como la ve el negocio (N05). */
public record IncidenciaNegocioResponse(
        Long id,
        TipoIncidencia tipo,
        EstadoIncidencia estado,
        OrigenIncidencia origen,
        String descripcion,
        String respuesta,
        OffsetDateTime fechaCreacion,
        OffsetDateTime fechaResolucion,
        Autor autor,
        PedidoRef pedido) {

    public record Autor(String nombre) {
    }

    public record Cliente(String nombre, String telefono) {
    }

    public record PedidoRef(Long id, String codigo, BigDecimal total, MetodoPago metodoPago, Cliente cliente) {
    }

    public static IncidenciaNegocioResponse de(Incidencia i) {
        Pedido p = i.getPedido();
        return new IncidenciaNegocioResponse(i.getId(), i.getTipo(), i.getEstado(), i.getOrigen(), i.getDescripcion(),
                i.getRespuesta(), i.getFechaCreacion(), i.getFechaResolucion(), new Autor(i.getAutor().getNombre()),
                new PedidoRef(p.getId(), p.getCodigo(), p.getTotal(), p.getMetodoPago(),
                        new Cliente(p.getCliente().getNombre(), p.getTelefonoContacto())));
    }
}
