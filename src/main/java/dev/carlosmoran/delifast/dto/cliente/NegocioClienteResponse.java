package dev.carlosmoran.delifast.dto.cliente;

import dev.carlosmoran.delifast.domain.Negocio;
import dev.carlosmoran.delifast.dto.LocalidadResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Negocio tal como lo ve el cliente en la lista (RF-05). */
public record NegocioClienteResponse(
        Long id,
        String nombre,
        String descripcion,
        String direccion,
        String telefono,
        LocalidadResponse localidad,
        String logoUrl,
        BigDecimal gastosEnvio,
        BigDecimal pedidoMinimo,
        boolean abierto) {

    public static NegocioClienteResponse de(Negocio n, LocalDateTime ahora) {
        return new NegocioClienteResponse(n.getId(), n.getNombre(), n.getDescripcion(), n.getDireccion(),
                n.getTelefono(), LocalidadResponse.de(n.getLocalidad()), n.getLogoUrl(), n.getGastosEnvio(),
                n.getPedidoMinimo(), n.estaAbierto(ahora));
    }
}
