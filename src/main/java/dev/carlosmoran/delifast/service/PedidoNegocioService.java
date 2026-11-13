package dev.carlosmoran.delifast.service;

import dev.carlosmoran.delifast.domain.EstadoPedido;
import dev.carlosmoran.delifast.domain.Pedido;
import dev.carlosmoran.delifast.domain.Repartidor;
import dev.carlosmoran.delifast.domain.Reparto;
import dev.carlosmoran.delifast.domain.Usuario;
import dev.carlosmoran.delifast.dto.negocio.PedidoNegocioResponse;
import dev.carlosmoran.delifast.exception.ConflictoException;
import dev.carlosmoran.delifast.exception.RecursoNoEncontradoException;
import dev.carlosmoran.delifast.exception.ReglaNegocioException;
import dev.carlosmoran.delifast.repository.PedidoRepository;
import dev.carlosmoran.delifast.repository.RepartidorRepository;
import dev.carlosmoran.delifast.repository.RepartoRepository;
import dev.carlosmoran.delifast.repository.UsuarioRepository;
import dev.carlosmoran.delifast.security.UsuarioAutenticado;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pedidos del negocio (RF-14, RF-15, RF-16, RF-20). Cada acción bloquea el pedido, comprueba
 * que sigue en el estado que exige (si no, 409) y guarda el cambio en el historial.
 * Un pedido de otro negocio responde 404.
 */
@Service
public class PedidoNegocioService {

    /** Las cuatro columnas del tablero. */
    static final Set<EstadoPedido> ACTIVOS = EnumSet.of(
            EstadoPedido.RECIBIDO, EstadoPedido.EN_PREPARACION, EstadoPedido.LISTO, EstadoPedido.EN_REPARTO);

    private final NegocioActual negocioActual;
    private final PedidoRepository pedidos;
    private final RepartidorRepository repartidores;
    private final RepartoRepository repartos;
    private final UsuarioRepository usuarios;

    public PedidoNegocioService(NegocioActual negocioActual, PedidoRepository pedidos,
                                RepartidorRepository repartidores, RepartoRepository repartos,
                                UsuarioRepository usuarios) {
        this.negocioActual = negocioActual;
        this.pedidos = pedidos;
        this.repartidores = repartidores;
        this.repartos = repartos;
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public List<PedidoNegocioResponse> listar(UsuarioAutenticado usuario, List<EstadoPedido> estados) {
        Long negocioId = negocioActual.id(usuario);
        Set<EstadoPedido> filtro = estados == null || estados.isEmpty() ? ACTIVOS : EnumSet.copyOf(estados);
        return pedidos.findByNegocioIdAndEstadoInOrderByFechaCreacionAsc(negocioId, filtro).stream()
                .map(PedidoNegocioResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public PedidoNegocioResponse obtener(UsuarioAutenticado usuario, Long pedidoId) {
        Long negocioId = negocioActual.id(usuario);
        return pedidos.findByIdAndNegocioId(pedidoId, negocioId)
                .map(PedidoNegocioResponse::de)
                .orElseThrow(() -> noEncontrado(pedidoId));
    }

    /** RECIBIDO → EN_PREPARACION con el tiempo estimado que verá el cliente. */
    @Transactional
    public PedidoNegocioResponse aceptar(UsuarioAutenticado usuario, Long pedidoId, int tiempoEstimadoMin) {
        Pedido pedido = bloquear(usuario, pedidoId, EstadoPedido.RECIBIDO);
        pedido.setTiempoEstimadoMin(tiempoEstimadoMin);
        pedido.cambiarEstado(EstadoPedido.EN_PREPARACION, autor(usuario));
        return PedidoNegocioResponse.de(pedido);
    }

    /** RECIBIDO → RECHAZADO con el motivo que verá el cliente. */
    @Transactional
    public PedidoNegocioResponse rechazar(UsuarioAutenticado usuario, Long pedidoId, String motivo) {
        Pedido pedido = bloquear(usuario, pedidoId, EstadoPedido.RECIBIDO);
        pedido.setMotivoRechazo(motivo.trim());
        pedido.cambiarEstado(EstadoPedido.RECHAZADO, autor(usuario));
        return PedidoNegocioResponse.de(pedido);
    }

    /** EN_PREPARACION → LISTO: queda pendiente de asignar repartidor. */
    @Transactional
    public PedidoNegocioResponse marcarListo(UsuarioAutenticado usuario, Long pedidoId) {
        Pedido pedido = bloquear(usuario, pedidoId, EstadoPedido.EN_PREPARACION);
        pedido.cambiarEstado(EstadoPedido.LISTO, autor(usuario));
        return PedidoNegocioResponse.de(pedido);
    }

    /**
     * LISTO → EN_REPARTO (regla del 06-11-2026: el negocio elige el repartidor). El repartidor
     * debe ser del mismo negocio, estar activo y disponible; si no, 422.
     */
    @Transactional
    public PedidoNegocioResponse asignar(UsuarioAutenticado usuario, Long pedidoId, UUID repartidorId) {
        Pedido pedido = bloquear(usuario, pedidoId, EstadoPedido.LISTO);
        Repartidor repartidor = repartidores.findById(repartidorId)
                .filter(r -> r.getNegocio() != null && r.getNegocio().getId().equals(pedido.getNegocio().getId()))
                .orElseThrow(() -> new ReglaNegocioException("El repartidor no pertenece a este negocio"));
        if (!repartidor.isActivo()) {
            throw new ReglaNegocioException("El repartidor está desactivado");
        }
        if (!repartidor.isDisponible()) {
            throw new ReglaNegocioException("El repartidor no está disponible");
        }
        Reparto reparto = pedido.asignarRepartidor(repartidor, autor(usuario));
        repartos.save(reparto);
        return PedidoNegocioResponse.de(pedido);
    }

    private Pedido bloquear(UsuarioAutenticado usuario, Long pedidoId, EstadoPedido estadoExigido) {
        Long negocioId = negocioActual.id(usuario);
        Pedido pedido = pedidos.bloquear(pedidoId, negocioId).orElseThrow(() -> noEncontrado(pedidoId));
        if (pedido.getEstado() != estadoExigido) {
            throw new ConflictoException("El pedido " + pedido.getCodigo() + " ya no está " + estadoExigido
                    + ": ahora está " + pedido.getEstado());
        }
        return pedido;
    }

    private Usuario autor(UsuarioAutenticado usuario) {
        return usuarios.getReferenceById(usuario.id());
    }

    private static RecursoNoEncontradoException noEncontrado(Long pedidoId) {
        return new RecursoNoEncontradoException("No existe el pedido " + pedidoId);
    }
}
