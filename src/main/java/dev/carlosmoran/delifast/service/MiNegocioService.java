package dev.carlosmoran.delifast.service;

import dev.carlosmoran.delifast.domain.EstadoPedido;
import dev.carlosmoran.delifast.domain.Negocio;
import dev.carlosmoran.delifast.domain.Repartidor;
import dev.carlosmoran.delifast.dto.LocalidadResponse;
import dev.carlosmoran.delifast.dto.negocio.MiNegocioResponse;
import dev.carlosmoran.delifast.dto.negocio.RepartidorNegocioResponse;
import dev.carlosmoran.delifast.repository.PedidoRepository;
import dev.carlosmoran.delifast.repository.RepartidorRepository;
import dev.carlosmoran.delifast.repository.RepartoRepository;
import dev.carlosmoran.delifast.security.UsuarioAutenticado;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Datos del propio negocio para su panel: cabecera, interruptor abierto y repartidores. */
@Service
public class MiNegocioService {

    private final NegocioActual negocioActual;
    private final PedidoRepository pedidos;
    private final RepartidorRepository repartidores;
    private final RepartoRepository repartos;
    private final Clock clock;

    public MiNegocioService(NegocioActual negocioActual, PedidoRepository pedidos, RepartidorRepository repartidores,
                            RepartoRepository repartos, Clock clock) {
        this.negocioActual = negocioActual;
        this.pedidos = pedidos;
        this.repartidores = repartidores;
        this.repartos = repartos;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MiNegocioResponse obtener(UsuarioAutenticado usuario) {
        return aRespuesta(negocioActual.cargar(usuario));
    }

    /** Interruptor "Abierto" de la cabecera (RF-18). */
    @Transactional
    public MiNegocioResponse cambiarAbierto(UsuarioAutenticado usuario, boolean abierto) {
        Negocio negocio = negocioActual.cargar(usuario);
        negocio.setAbiertoManual(abierto);
        return aRespuesta(negocio);
    }

    @Transactional(readOnly = true)
    public List<RepartidorNegocioResponse> repartidores(UsuarioAutenticado usuario, Boolean soloDisponibles) {
        Long negocioId = negocioActual.id(usuario);
        List<Repartidor> lista = Boolean.TRUE.equals(soloDisponibles)
                ? repartidores.findByNegocioIdAndActivoTrueAndDisponibleTrueOrderByNombreAsc(negocioId)
                : repartidores.findByNegocioIdOrderByNombreAsc(negocioId);
        Map<UUID, Long> enReparto = repartos.contarEnRepartoPorRepartidor(negocioId).stream()
                .collect(Collectors.toMap(RepartoRepository.RepartosPorRepartidor::getRepartidorId,
                        RepartoRepository.RepartosPorRepartidor::getRepartos));
        return lista.stream()
                .map(r -> new RepartidorNegocioResponse(r.getId(), r.getNombre(), r.getEmail(), r.getTelefono(),
                        r.isActivo(), r.isDisponible(), enReparto.getOrDefault(r.getId(), 0L)))
                .toList();
    }

    private MiNegocioResponse aRespuesta(Negocio n) {
        long pedidosHoy = pedidos.countByNegocioIdAndFechaCreacionGreaterThanEqualAndEstadoNot(
                n.getId(), Hoy.inicio(clock), EstadoPedido.CANCELADO);
        return new MiNegocioResponse(n.getId(), n.getNombre(), LocalidadResponse.de(n.getLocalidad()),
                n.estaAbierto(Hoy.ahoraLocal(clock)), n.isAbiertoManual(), pedidosHoy);
    }
}
