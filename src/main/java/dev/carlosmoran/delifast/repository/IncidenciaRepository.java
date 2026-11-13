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

    /** Incidencias de los pedidos de un negocio, con autor, pedido y cliente. */
    @Query("""
            select i from Incidencia i
            join fetch i.autor
            join fetch i.pedido p
            join fetch p.cliente
            where p.negocio.id = :negocioId and i.estado in :estados
            order by i.fechaCreacion desc""")
    List<Incidencia> buscarDeNegocio(@Param("negocioId") Long negocioId,
                                     @Param("estados") Collection<EstadoIncidencia> estados);

    @Query("select i from Incidencia i join fetch i.pedido p where i.id = :id and p.negocio.id = :negocioId")
    java.util.Optional<Incidencia> buscarDeNegocio(@Param("id") Long id, @Param("negocioId") Long negocioId);

    /** Incidencias comunicadas en un periodo por estado, para las estadísticas. */
    interface IncidenciasPorEstado {
        EstadoIncidencia getEstado();

        long getTotal();
    }

    @Query("""
            select i.estado as estado, count(i) as total from Incidencia i
            where i.pedido.negocio.id = :negocioId and i.fechaCreacion >= :desde and i.fechaCreacion < :hasta
            group by i.estado""")
    List<IncidenciasPorEstado> contarPorEstado(@Param("negocioId") Long negocioId,
                                               @Param("desde") OffsetDateTime desde,
                                               @Param("hasta") OffsetDateTime hasta);

    long countByEstadoIn(Collection<EstadoIncidencia> estados);

    long countByEstadoInAndFechaCreacionGreaterThanEqual(Collection<EstadoIncidencia> estados, OffsetDateTime desde);
}
