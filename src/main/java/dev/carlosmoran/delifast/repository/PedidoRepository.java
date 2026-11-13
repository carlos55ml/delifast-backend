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

    // ---- Estadísticas (RF-22) ----

    /** Fechas de los pedidos que cuentan (ni rechazados ni cancelados) de un periodo. */
    @Query("""
            select p.fechaCreacion from Pedido p
            where p.negocio.id = :negocioId and p.fechaCreacion >= :desde and p.fechaCreacion < :hasta
              and p.estado not in (dev.carlosmoran.delifast.domain.EstadoPedido.RECHAZADO,
                                   dev.carlosmoran.delifast.domain.EstadoPedido.CANCELADO)""")
    List<OffsetDateTime> fechasDePedidos(@Param("negocioId") Long negocioId, @Param("desde") OffsetDateTime desde,
                                         @Param("hasta") OffsetDateTime hasta);

    @Query("""
            select coalesce(sum(p.total), 0) from Pedido p
            where p.negocio.id = :negocioId and p.fechaCreacion >= :desde and p.fechaCreacion < :hasta
              and p.estado = dev.carlosmoran.delifast.domain.EstadoPedido.ENTREGADO""")
    java.math.BigDecimal facturacion(@Param("negocioId") Long negocioId, @Param("desde") OffsetDateTime desde,
                                     @Param("hasta") OffsetDateTime hasta);

    /** Minutos medios desde que se recibe el pedido hasta que se entrega; null sin entregas. */
    @Query(value = """
            select cast(avg(extract(epoch from (r.fecha_entrega - p.fecha_creacion))) / 60 as double precision)
            from pedidos p join repartos r on r.pedido_id = p.id
            where p.negocio_id = :negocioId and p.fecha_creacion >= :desde and p.fecha_creacion < :hasta
              and p.estado = 'ENTREGADO' and r.fecha_entrega is not null""", nativeQuery = true)
    Double minutosMediosEntrega(@Param("negocioId") Long negocioId, @Param("desde") OffsetDateTime desde,
                                @Param("hasta") OffsetDateTime hasta);

    interface UnidadesProducto {
        String getNombre();

        long getUnidades();
    }

    /** Productos más vendidos por unidades (por el nombre copiado en la línea). */
    @Query("""
            select l.nombreProducto as nombre, sum(l.cantidad) as unidades from LineaPedido l
            where l.pedido.negocio.id = :negocioId
              and l.pedido.fechaCreacion >= :desde and l.pedido.fechaCreacion < :hasta
              and l.pedido.estado not in (dev.carlosmoran.delifast.domain.EstadoPedido.RECHAZADO,
                                          dev.carlosmoran.delifast.domain.EstadoPedido.CANCELADO)
            group by l.nombreProducto
            order by sum(l.cantidad) desc, l.nombreProducto""")
    List<UnidadesProducto> masVendidos(@Param("negocioId") Long negocioId, @Param("desde") OffsetDateTime desde,
                                       @Param("hasta") OffsetDateTime hasta, org.springframework.data.domain.Limit limite);

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
