package dev.carlosmoran.delifast.repository;

import dev.carlosmoran.delifast.domain.EstadoPedido;
import dev.carlosmoran.delifast.domain.Pedido;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    /** Número de pedidos creados desde un momento, agrupados por negocio. */
    interface PedidosPorNegocio {
        Long getNegocioId();

        long getPedidos();
    }

    @Query("""
            select p.negocio.id as negocioId, count(p) as pedidos
            from Pedido p where p.fechaCreacion >= :desde
            group by p.negocio.id""")
    List<PedidosPorNegocio> contarPorNegocioDesde(@Param("desde") OffsetDateTime desde);

    long countByFechaCreacionGreaterThanEqual(OffsetDateTime desde);

    long countByNegocioIdAndFechaCreacionGreaterThanEqualAndEstadoNot(Long negocioId, OffsetDateTime desde,
                                                                       EstadoPedido estado);

    /** Tablero del negocio. Líneas e historial se cargan por lotes (default_batch_fetch_size). */
    @EntityGraph(attributePaths = {"cliente"})
    List<Pedido> findByNegocioIdAndEstadoInOrderByFechaCreacionAsc(Long negocioId, Collection<EstadoPedido> estados);

    @EntityGraph(attributePaths = {"cliente"})
    Optional<Pedido> findByIdAndNegocioId(Long id, Long negocioId);

    /**
     * Pedido del negocio bloqueado hasta el final de la transacción (SELECT ... FOR UPDATE):
     * si dos personas del negocio actúan a la vez sobre él, la segunda espera y ve el estado nuevo.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pedido p where p.id = :id and p.negocio.id = :negocioId")
    Optional<Pedido> bloquear(@Param("id") Long id, @Param("negocioId") Long negocioId);

    @Query("select count(distinct p.negocio.localidad.id) from Pedido p where p.fechaCreacion >= :desde")
    long contarLocalidadesConPedidosDesde(@Param("desde") OffsetDateTime desde);
}
