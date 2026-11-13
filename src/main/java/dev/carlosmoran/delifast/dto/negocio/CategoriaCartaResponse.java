package dev.carlosmoran.delifast.dto.negocio;

import dev.carlosmoran.delifast.domain.Categoria;
import dev.carlosmoran.delifast.domain.Producto;
import java.util.Comparator;
import java.util.List;

/** Categoría con sus productos no borrados, por nombre (incluidos los agotados). */
public record CategoriaCartaResponse(Long id, String nombre, int orden, List<ProductoCartaResponse> productos) {

    public static CategoriaCartaResponse de(Categoria c) {
        return new CategoriaCartaResponse(c.getId(), c.getNombre(), c.getOrden(),
                c.getProductos().stream()
                        .filter(Producto::isActivo)
                        .sorted(Comparator.comparing(Producto::getNombre, String.CASE_INSENSITIVE_ORDER))
                        .map(ProductoCartaResponse::de)
                        .toList());
    }
}
