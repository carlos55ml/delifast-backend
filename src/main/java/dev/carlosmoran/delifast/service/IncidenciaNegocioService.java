package dev.carlosmoran.delifast.service;

import dev.carlosmoran.delifast.domain.EstadoIncidencia;
import dev.carlosmoran.delifast.domain.Incidencia;
import dev.carlosmoran.delifast.dto.negocio.IncidenciaNegocioResponse;
import dev.carlosmoran.delifast.exception.ConflictoException;
import dev.carlosmoran.delifast.exception.RecursoNoEncontradoException;
import dev.carlosmoran.delifast.repository.IncidenciaRepository;
import dev.carlosmoran.delifast.repository.UsuarioRepository;
import dev.carlosmoran.delifast.security.UsuarioAutenticado;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Incidencias de los pedidos del negocio (N05, RF-21): el negocio responde (→ RESPONDIDA) y la
 * marca como resuelta (→ RESUELTA). Quien lo hace queda como responsable. Una incidencia de un
 * pedido de otro negocio responde 404.
 */
@Service
public class IncidenciaNegocioService {

    private final NegocioActual negocioActual;
    private final IncidenciaRepository incidencias;
    private final UsuarioRepository usuarios;

    public IncidenciaNegocioService(NegocioActual negocioActual, IncidenciaRepository incidencias,
                                    UsuarioRepository usuarios) {
        this.negocioActual = negocioActual;
        this.incidencias = incidencias;
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public List<IncidenciaNegocioResponse> listar(UsuarioAutenticado usuario, List<EstadoIncidencia> estados) {
        Set<EstadoIncidencia> filtro = estados == null || estados.isEmpty()
                ? EnumSet.allOf(EstadoIncidencia.class) : EnumSet.copyOf(estados);
        return incidencias.buscarDeNegocio(negocioActual.id(usuario), filtro).stream()
                .map(IncidenciaNegocioResponse::de)
                .toList();
    }

    /** Si ya estaba respondida, sustituye la respuesta. */
    @Transactional
    public IncidenciaNegocioResponse responder(UsuarioAutenticado usuario, Long id, String respuesta) {
        Incidencia incidencia = sinResolver(usuario, id);
        incidencia.responder(usuarios.getReferenceById(usuario.id()), respuesta.trim());
        return IncidenciaNegocioResponse.de(incidencia);
    }

    @Transactional
    public IncidenciaNegocioResponse resolver(UsuarioAutenticado usuario, Long id, String respuesta) {
        Incidencia incidencia = sinResolver(usuario, id);
        incidencia.resolver(usuarios.getReferenceById(usuario.id()), respuesta == null ? null : respuesta.trim());
        return IncidenciaNegocioResponse.de(incidencia);
    }

    private Incidencia sinResolver(UsuarioAutenticado usuario, Long id) {
        Incidencia incidencia = incidencias.buscarDeNegocio(id, negocioActual.id(usuario))
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la incidencia " + id));
        if (incidencia.getEstado() == EstadoIncidencia.RESUELTA) {
            throw new ConflictoException("La incidencia ya está resuelta");
        }
        return incidencia;
    }
}
