package dev.carlosmoran.delifast.dto.cliente;

import dev.carlosmoran.delifast.domain.Categoria;
import dev.carlosmoran.delifast.domain.Producto;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/** Carta de un negocio para el cliente (RF-06). */
public record CartaClienteResponse(NegocioClienteResponse negocio, List<CategoriaCliente> categorias) {

    public record CategoriaCliente(Long id, String nombre, List<ProductoCliente> productos) {
    }

    public record ProductoCliente(Long id, String nombre, String descripcion, BigDecimal precio, String fotoUrl,
                                  boolean disponible) {
    }

    /** Categoría con sus productos no borrados en orden de creación; null si no le queda ninguno. */
    public static CategoriaCliente categoria(Categoria c) {
        List<ProductoCliente> productos = c.getProductos().stream()
                .filter(Producto::isActivo)
                .sorted(Comparator.comparing(Producto::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(p -> new ProductoCliente(p.getId(), p.getNombre(), p.getDescripcion(), p.getPrecio(),
                        p.getFotoUrl(), p.isDisponible()))
                .toList();
        return productos.isEmpty() ? null : new CategoriaCliente(c.getId(), c.getNombre(), productos);
    }
}
