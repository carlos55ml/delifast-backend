package dev.carlosmoran.delifast.repository;

import dev.carlosmoran.delifast.domain.Repartidor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepartidorRepository extends JpaRepository<Repartidor, UUID> {

    List<Repartidor> findByNegocioId(Long negocioId);

    List<Repartidor> findByNegocioIdOrderByNombreAsc(Long negocioId);

    Optional<Repartidor> findByIdAndNegocioId(UUID id, Long negocioId);

    List<Repartidor> findByNegocioIdAndActivoTrueAndDisponibleTrueOrderByNombreAsc(Long negocioId);
}
