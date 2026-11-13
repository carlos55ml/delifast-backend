package dev.carlosmoran.delifast.repository;

import dev.carlosmoran.delifast.domain.Producto;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    /** Producto no borrado de una categoría del negocio. */
    Optional<Producto> findByIdAndCategoriaNegocioIdAndActivoTrue(Long id, Long negocioId);

    boolean existsByCategoriaIdAndActivoTrue(Long categoriaId);
}
