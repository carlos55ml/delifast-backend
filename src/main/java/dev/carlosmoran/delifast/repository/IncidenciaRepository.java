package dev.carlosmoran.delifast.repository;

import dev.carlosmoran.delifast.domain.EstadoIncidencia;
import dev.carlosmoran.delifast.domain.Incidencia;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IncidenciaRepository extends JpaRepository<Incidencia, Long> {

    /** Incidencias con su pedido y negocio, de la más reciente a la más antigua. */
    @Query("""
            select i from Incidencia i
            join fetch i.pedido p
            join fetch p.negocio n
            where i.estado in :estados
              and (:negocioId is null or n.id = :negocioId)
            order by i.fechaCreacion desc""")
    List<Incidencia> buscar(@Param("estados") Collection<EstadoIncidencia> estados,
                            @Param("negocioId") Long negocioId,
                            Limit limite);

    long countByEstadoIn(Collection<EstadoIncidencia> estados);

    long countByEstadoInAndFechaCreacionGreaterThanEqual(Collection<EstadoIncidencia> estados, OffsetDateTime desde);
}
