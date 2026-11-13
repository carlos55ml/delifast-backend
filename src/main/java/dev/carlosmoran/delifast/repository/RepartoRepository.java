package dev.carlosmoran.delifast.repository;

import dev.carlosmoran.delifast.domain.Reparto;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepartoRepository extends JpaRepository<Reparto, Long> {

    /** Repartidores con algún pedido EN_REPARTO ahora mismo. */
    @Query("""
            select count(distinct r.repartidor.id) from Reparto r
            where r.pedido.estado = dev.carlosmoran.delifast.domain.EstadoPedido.EN_REPARTO""")
    long contarRepartidoresEnRuta();

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
}
