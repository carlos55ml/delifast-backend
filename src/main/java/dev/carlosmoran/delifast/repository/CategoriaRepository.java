package dev.carlosmoran.delifast.repository;

import dev.carlosmoran.delifast.domain.Categoria;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    /** Carta del negocio; los productos se cargan por lotes (default_batch_fetch_size). */
    List<Categoria> findByNegocioIdOrderByOrdenAscIdAsc(Long negocioId);

    Optional<Categoria> findByIdAndNegocioId(Long id, Long negocioId);

    boolean existsByNegocioIdAndNombreIgnoreCase(Long negocioId, String nombre);

    boolean existsByNegocioIdAndNombreIgnoreCaseAndIdNot(Long negocioId, String nombre, Long id);

    @Query("select coalesce(max(c.orden), -1) from Categoria c where c.negocio.id = :negocioId")
    int ultimoOrden(@Param("negocioId") Long negocioId);
}
