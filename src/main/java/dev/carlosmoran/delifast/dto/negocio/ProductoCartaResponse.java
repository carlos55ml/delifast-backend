package dev.carlosmoran.delifast.dto.negocio;

import dev.carlosmoran.delifast.domain.Producto;
import java.math.BigDecimal;

public record ProductoCartaResponse(
        Long id,
        Long categoriaId,
        String nombre,
        String descripcion,
        BigDecimal precio,
        String fotoUrl,
        boolean disponible) {

    public static ProductoCartaResponse de(Producto p) {
        return new ProductoCartaResponse(p.getId(), p.getCategoria().getId(), p.getNombre(), p.getDescripcion(),
                p.getPrecio(), p.getFotoUrl(), p.isDisponible());
    }
}
