package dev.carlosmoran.delifast.repository;

import dev.carlosmoran.delifast.domain.Pedido;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
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

    @Query("select count(distinct p.negocio.localidad.id) from Pedido p where p.fechaCreacion >= :desde")
    long contarLocalidadesConPedidosDesde(@Param("desde") OffsetDateTime desde);
}
