package dev.carlosmoran.delifast.repository;

import dev.carlosmoran.delifast.domain.Direccion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DireccionRepository extends JpaRepository<Direccion, Long> {

    @EntityGraph(attributePaths = {"localidad"})
    List<Direccion> findByClienteIdOrderByPredeterminadaDescAliasAsc(UUID clienteId);

    @EntityGraph(attributePaths = {"localidad"})
    Optional<Direccion> findByIdAndClienteId(Long id, UUID clienteId);

    boolean existsByClienteId(UUID clienteId);
}
