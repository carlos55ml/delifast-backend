package dev.carlosmoran.delifast.service;

import dev.carlosmoran.delifast.domain.EstadoPedido;
import dev.carlosmoran.delifast.domain.Incidencia;
import dev.carlosmoran.delifast.domain.OrigenIncidencia;
import dev.carlosmoran.delifast.domain.Pedido;
import dev.carlosmoran.delifast.domain.Repartidor;
import dev.carlosmoran.delifast.domain.Reparto;
import dev.carlosmoran.delifast.domain.MetodoPago;
import dev.carlosmoran.delifast.domain.TipoIncidencia;
import dev.carlosmoran.delifast.dto.negocio.IncidenciaNegocioResponse;
import dev.carlosmoran.delifast.dto.repartidor.PerfilRepartidorResponse;
import dev.carlosmoran.delifast.dto.repartidor.RepartoRepartidorResponse;
import dev.carlosmoran.delifast.exception.ConflictoException;
import dev.carlosmoran.delifast.exception.RecursoNoEncontradoException;
import dev.carlosmoran.delifast.repository.IncidenciaRepository;
import dev.carlosmoran.delifast.repository.RepartidorRepository;
import dev.carlosmoran.delifast.repository.RepartoRepository;
import dev.carlosmoran.delifast.security.UsuarioAutenticado;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Vista del repartidor (R01..R06, RF-23..RF-26). Solo ve y toca sus propios repartos; uno de otro
 * responde 404. Si su negocio está suspendido no puede operar aunque el token siga vigente.
 */
@Service
public class RepartidorService {

    private final RepartidorRepository repartidores;
    private final RepartoRepository repartos;
    private final IncidenciaRepository incidencias;
    private final Clock clock;

    public RepartidorService(RepartidorRepository repartidores, RepartoRepository repartos,
                             IncidenciaRepository incidencias, Clock clock) {
        this.repartidores = repartidores;
        this.repartos = repartos;
        this.incidencias = incidencias;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PerfilRepartidorResponse perfil(UsuarioAutenticado usuario) {
        return perfil(repartidor(usuario));
    }

    /** El negocio solo le asigna pedidos si está disponible (N08). */
    @Transactional
    public PerfilRepartidorResponse cambiarDisponible(UsuarioAutenticado usuario, boolean disponible) {
        Repartidor repartidor = repartidor(usuario);
        repartidor.cambiarDisponibilidad(disponible);
        return perfil(repartidor);
    }

    /** EN_REPARTO = por entregar; ENTREGADO = entregados hoy; sin estado, los dos. */
    @Transactional(readOnly = true)
    public List<RepartoRepartidorResponse> repartos(UsuarioAutenticado usuario, EstadoPedido estado) {
        Repartidor repartidor = repartidor(usuario);
        Stream<Reparto> enReparto = estado == EstadoPedido.ENTREGADO ? Stream.empty()
                : repartos.findByRepartidorIdAndPedidoEstadoOrderByFechaAsignacionDesc(repartidor.getId(),
                        EstadoPedido.EN_REPARTO).stream();
        Stream<Reparto> entregados = estado == EstadoPedido.EN_REPARTO ? Stream.empty()
                : repartos.findByRepartidorIdAndPedidoEstadoAndFechaEntregaGreaterThanEqualOrderByFechaAsignacionDesc(
                        repartidor.getId(), EstadoPedido.ENTREGADO, Hoy.inicio(clock)).stream();
        return Stream.concat(enReparto, entregados)
                .sorted(Comparator.comparing(Reparto::getFechaAsignacion).reversed())
                .map(RepartoRepartidorResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public RepartoRepartidorResponse reparto(UsuarioAutenticado usuario, Long id) {
        Repartidor repartidor = repartidor(usuario);
        return repartos.findByIdAndRepartidorId(id, repartidor.getId())
                .map(RepartoRepartidorResponse::de)
                .orElseThrow(() -> noEncontrado(id));
    }

    /**
     * Confirma entrega y cobro (RF-25): pedido a ENTREGADO, hora de entrega y método real de cobro.
     * Si el pedido ya no está EN_REPARTO (doble pulsación), 409.
     */
    @Transactional
    public RepartoRepartidorResponse entregar(UsuarioAutenticado usuario, Long id, MetodoPago metodoCobro) {
        Repartidor repartidor = repartidor(usuario);
        Reparto reparto = repartos.bloquear(id, repartidor.getId()).orElseThrow(() -> noEncontrado(id));
        Pedido pedido = reparto.getPedido();
        if (pedido.getEstado() != EstadoPedido.EN_REPARTO) {
            throw new ConflictoException("El pedido " + pedido.getCodigo() + " ya no está EN_REPARTO: ahora está "
                    + pedido.getEstado());
        }
        reparto.confirmarEntrega(metodoCobro);
        pedido.cambiarEstado(EstadoPedido.ENTREGADO, repartidor);
        return RepartoRepartidorResponse.de(reparto);
    }

    /** Incidencia de reparto (RF-26): origen REPARTIDOR; no cambia el estado del pedido. */
    @Transactional
    public IncidenciaNegocioResponse comunicarIncidencia(UsuarioAutenticado usuario, Long id, TipoIncidencia tipo,
                                                         String descripcion) {
        Repartidor repartidor = repartidor(usuario);
        Reparto reparto = repartos.findByIdAndRepartidorId(id, repartidor.getId()).orElseThrow(() -> noEncontrado(id));
        String texto = descripcion == null || descripcion.isBlank() ? describir(tipo) : descripcion.trim();
        Incidencia incidencia = incidencias.save(
                new Incidencia(reparto.getPedido(), repartidor, OrigenIncidencia.REPARTIDOR, tipo, texto));
        return IncidenciaNegocioResponse.de(incidencia);
    }

    private PerfilRepartidorResponse perfil(Repartidor r) {
        OffsetDateTime hoy = Hoy.inicio(clock);
        return new PerfilRepartidorResponse(r.getId(), r.getNombre(), r.getEmail(), r.getTelefono(),
                new PerfilRepartidorResponse.NegocioRef(r.getNegocio().getId(), r.getNegocio().getNombre(),
                        r.getNegocio().getLocalidad().getNombre()),
                r.isDisponible(),
                repartos.countByRepartidorIdAndFechaAsignacionGreaterThanEqual(r.getId(), hoy),
                repartos.countByRepartidorIdAndPedidoEstado(r.getId(), EstadoPedido.EN_REPARTO));
    }

    private Repartidor repartidor(UsuarioAutenticado usuario) {
        Repartidor repartidor = repartidores.findById(usuario.id())
                .orElseThrow(() -> new AccessDeniedException("La cuenta no es de repartidor"));
        if (!repartidor.isActivo() || repartidor.getNegocio() == null || !repartidor.getNegocio().isActivo()) {
            throw new AccessDeniedException("La cuenta o su negocio están desactivados");
        }
        return repartidor;
    }

    static String describir(TipoIncidencia tipo) {
        return switch (tipo) {
            case FALTA_PRODUCTO -> "Falta un producto";
            case RETRASO -> "Retraso en la entrega";
            case PRODUCTO_MAL_ESTADO -> "Producto en mal estado";
            case CLIENTE_AUSENTE -> "El cliente no está en la dirección";
            case DIRECCION_INCORRECTA -> "La dirección es incorrecta";
            case OTRA -> "Otra incidencia";
        };
    }

    private static RecursoNoEncontradoException noEncontrado(Long id) {
        return new RecursoNoEncontradoException("No existe el reparto " + id);
    }
}
