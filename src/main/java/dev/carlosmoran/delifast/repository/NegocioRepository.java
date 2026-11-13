package dev.carlosmoran.delifast.repository;

import dev.carlosmoran.delifast.domain.Negocio;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NegocioRepository extends JpaRepository<Negocio, Long> {

    @EntityGraph(attributePaths = {"localidad", "horarios"})
    List<Negocio> findAllByOrderByNombreAsc();

    @EntityGraph(attributePaths = {"localidad", "horarios"})
    List<Negocio> findByActivoOrderByNombreAsc(boolean activo);

    @EntityGraph(attributePaths = {"localidad", "horarios"})
    List<Negocio> findByActivoTrueAndLocalidadIdOrderByNombreAsc(Long localidadId);

    @EntityGraph(attributePaths = {"localidad", "horarios"})
    java.util.Optional<Negocio> findByIdAndActivoTrue(Long id);

}
