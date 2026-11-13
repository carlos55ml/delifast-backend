package dev.carlosmoran.delifast.service;

import dev.carlosmoran.delifast.domain.Negocio;
import dev.carlosmoran.delifast.repository.NegocioRepository;
import dev.carlosmoran.delifast.security.UsuarioAutenticado;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Negocio del usuario autenticado (claim negocio_id). Un negocio suspendido no puede operar
 * aunque su access token aún no haya caducado: el hook solo lo impide al renovar la sesión.
 */
@Component
public class NegocioActual {

    private final NegocioRepository negocios;

    public NegocioActual(NegocioRepository negocios) {
        this.negocios = negocios;
    }

    public Negocio cargar(UsuarioAutenticado usuario) {
        if (usuario.negocioId() == null) {
            throw new AccessDeniedException("La cuenta no pertenece a ningún negocio");
        }
        Negocio negocio = negocios.findById(usuario.negocioId())
                .orElseThrow(() -> new AccessDeniedException("El negocio de la cuenta no existe"));
        if (!negocio.isActivo()) {
            throw new AccessDeniedException("El negocio está suspendido");
        }
        return negocio;
    }

    public Long id(UsuarioAutenticado usuario) {
        return cargar(usuario).getId();
    }
}
