package dev.carlosmoran.delifast.repository;

import dev.carlosmoran.delifast.domain.Localidad;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocalidadRepository extends JpaRepository<Localidad, Long> {

    List<Localidad> findAllByOrderByNombreAsc();
}
