package dev.carlosmoran.delifast.repository;

import dev.carlosmoran.delifast.domain.GestorNegocio;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GestorNegocioRepository extends JpaRepository<GestorNegocio, UUID> {

    @Query("select g from GestorNegocio g where g.esDueno = true and g.negocio.id in :negocioIds")
    List<GestorNegocio> findDuenos(@Param("negocioIds") Collection<Long> negocioIds);

    List<GestorNegocio> findByNegocioId(Long negocioId);
}
