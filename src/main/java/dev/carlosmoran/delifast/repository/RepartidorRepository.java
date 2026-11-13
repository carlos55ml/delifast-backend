package dev.carlosmoran.delifast.repository;

import dev.carlosmoran.delifast.domain.Repartidor;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepartidorRepository extends JpaRepository<Repartidor, UUID> {

    List<Repartidor> findByNegocioId(Long negocioId);
}
