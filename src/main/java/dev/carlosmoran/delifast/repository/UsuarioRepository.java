package dev.carlosmoran.delifast.repository;

import dev.carlosmoran.delifast.domain.Usuario;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    boolean existsByEmailIgnoreCase(String email);
}
