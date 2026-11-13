package dev.carlosmoran.delifast.dto.admin;

import dev.carlosmoran.delifast.domain.EstadoIncidencia;
import dev.carlosmoran.delifast.domain.Incidencia;
import dev.carlosmoran.delifast.domain.OrigenIncidencia;
import dev.carlosmoran.delifast.domain.Pedido;
import dev.carlosmoran.delifast.domain.TipoIncidencia;
import java.time.OffsetDateTime;

public record IncidenciaAdminResponse(
        Long id,
        TipoIncidencia tipo,
        EstadoIncidencia estado,
        OrigenIncidencia origen,
        String descripcion,
        String respuesta,
        OffsetDateTime fechaCreacion,
        OffsetDateTime fechaResolucion,
        PedidoRef pedido,
        NegocioRef negocio) {

    public record PedidoRef(Long id, String codigo) {
    }

    public record NegocioRef(Long id, String nombre) {
    }

    public static IncidenciaAdminResponse de(Incidencia i) {
        Pedido pedido = i.getPedido();
        return new IncidenciaAdminResponse(i.getId(), i.getTipo(), i.getEstado(), i.getOrigen(),
                i.getDescripcion(), i.getRespuesta(), i.getFechaCreacion(), i.getFechaResolucion(),
                new PedidoRef(pedido.getId(), pedido.getCodigo()),
                new NegocioRef(pedido.getNegocio().getId(), pedido.getNegocio().getNombre()));
    }
}
