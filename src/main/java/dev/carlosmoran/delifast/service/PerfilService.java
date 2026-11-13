package dev.carlosmoran.delifast.service;

import dev.carlosmoran.delifast.domain.GestorNegocio;
import dev.carlosmoran.delifast.domain.Negocio;
import dev.carlosmoran.delifast.domain.Repartidor;
import dev.carlosmoran.delifast.domain.Usuario;
import dev.carlosmoran.delifast.dto.PerfilResponse;
import dev.carlosmoran.delifast.exception.RecursoNoEncontradoException;
import dev.carlosmoran.delifast.repository.UsuarioRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PerfilService {

    private final UsuarioRepository usuarios;

    public PerfilService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public PerfilResponse obtener(UUID id) {
        Usuario usuario = usuarios.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el perfil del usuario"));
        Negocio negocio = switch (usuario) {
            case GestorNegocio g -> g.getNegocio();
            case Repartidor r -> r.getNegocio();
            default -> null;
        };
        boolean esDueno = usuario instanceof GestorNegocio g && g.isEsDueno();
        return new PerfilResponse(usuario.getId(), usuario.getNombre(), usuario.getEmail(), usuario.getTelefono(),
                usuario.getRol(), negocio == null ? null : negocio.getId(), esDueno);
    }
}
