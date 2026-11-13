package dev.carlosmoran.delifast.repository;

import dev.carlosmoran.delifast.domain.Reparto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RepartoRepository extends JpaRepository<Reparto, Long> {

    /** Repartidores con algún pedido EN_REPARTO ahora mismo. */
    @Query("""
            select count(distinct r.repartidor.id) from Reparto r
            where r.pedido.estado = dev.carlosmoran.delifast.domain.EstadoPedido.EN_REPARTO""")
    long contarRepartidoresEnRuta();
}
