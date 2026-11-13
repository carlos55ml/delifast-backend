package dev.carlosmoran.delifast.repository;

import dev.carlosmoran.delifast.domain.Reparto;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepartoRepository extends JpaRepository<Reparto, Long> {

    /** Repartidores con algún pedido EN_REPARTO ahora mismo. */
    @Query("""
            select count(distinct r.repartidor.id) from Reparto r
            where r.pedido.estado = dev.carlosmoran.delifast.domain.EstadoPedido.EN_REPARTO""")
    long contarRepartidoresEnRuta();

    // ---- Vista del repartidor (RF-23..RF-26) ----

    @EntityGraph(attributePaths = {"pedido", "pedido.cliente"})
    List<Reparto> findByRepartidorIdAndPedidoEstadoOrderByFechaAsignacionDesc(UUID repartidorId,
                                                                             dev.carlosmoran.delifast.domain.EstadoPedido estado);

    @EntityGraph(attributePaths = {"pedido", "pedido.cliente"})
    List<Reparto> findByRepartidorIdAndPedidoEstadoAndFechaEntregaGreaterThanEqualOrderByFechaAsignacionDesc(
            UUID repartidorId, dev.carlosmoran.delifast.domain.EstadoPedido estado, java.time.OffsetDateTime desde);

    @EntityGraph(attributePaths = {"pedido", "pedido.cliente"})
    java.util.Optional<Reparto> findByIdAndRepartidorId(Long id, UUID repartidorId);

    /** Reparto propio bloqueado hasta el final de la transacción, para confirmar la entrega una sola vez. */
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reparto r where r.id = :id and r.repartidor.id = :repartidorId")
    java.util.Optional<Reparto> bloquear(@Param("id") Long id, @Param("repartidorId") UUID repartidorId);

    long countByRepartidorIdAndFechaAsignacionGreaterThanEqual(UUID repartidorId, java.time.OffsetDateTime desde);

    long countByRepartidorIdAndPedidoEstado(UUID repartidorId, dev.carlosmoran.delifast.domain.EstadoPedido estado);

    /** Repartos EN_REPARTO de cada repartidor del negocio. */
    interface RepartosPorRepartidor {
        UUID getRepartidorId();

        long getRepartos();
    }

    @Query("""
            select r.repartidor.id as repartidorId, count(r) as repartos from Reparto r
            where r.pedido.estado = dev.carlosmoran.delifast.domain.EstadoPedido.EN_REPARTO
              and r.pedido.negocio.id = :negocioId
            group by r.repartidor.id""")
    List<RepartosPorRepartidor> contarEnRepartoPorRepartidor(@Param("negocioId") Long negocioId);

    /** Repartos asignados desde un momento (entregados o no) por repartidor del negocio. */
    @Query("""
            select r.repartidor.id as repartidorId, count(r) as repartos from Reparto r
            where r.fechaAsignacion >= :desde and r.pedido.negocio.id = :negocioId
            group by r.repartidor.id""")
    List<RepartosPorRepartidor> contarAsignadosDesde(@Param("negocioId") Long negocioId,
                                                     @Param("desde") java.time.OffsetDateTime desde);
}
