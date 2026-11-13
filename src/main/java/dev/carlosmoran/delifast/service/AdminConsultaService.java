package dev.carlosmoran.delifast.service;

import dev.carlosmoran.delifast.domain.EstadoIncidencia;
import dev.carlosmoran.delifast.domain.Negocio;
import dev.carlosmoran.delifast.dto.admin.IncidenciaAdminResponse;
import dev.carlosmoran.delifast.dto.admin.ResumenAdminResponse;
import dev.carlosmoran.delifast.dto.admin.ResumenAdminResponse.PedidosNegocioHoy;
import dev.carlosmoran.delifast.repository.IncidenciaRepository;
import dev.carlosmoran.delifast.repository.NegocioRepository;
import dev.carlosmoran.delifast.repository.PedidoRepository;
import dev.carlosmoran.delifast.repository.RepartoRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consultas de la zona de administración: incidencias (RF-30) y resumen (RF-31). */
@Service
@Transactional(readOnly = true)
public class AdminConsultaService {

    /** Incidencias aún sin resolver. */
    static final Set<EstadoIncidencia> SIN_RESOLVER = EnumSet.of(EstadoIncidencia.ABIERTA, EstadoIncidencia.RESPONDIDA);
    private static final int INCIDENCIAS_RECIENTES = 5;

    private final NegocioRepository negocios;
    private final PedidoRepository pedidos;
    private final IncidenciaRepository incidencias;
    private final RepartoRepository repartos;
    private final Clock clock;

    public AdminConsultaService(NegocioRepository negocios, PedidoRepository pedidos,
                                IncidenciaRepository incidencias, RepartoRepository repartos, Clock clock) {
        this.negocios = negocios;
        this.pedidos = pedidos;
        this.incidencias = incidencias;
        this.repartos = repartos;
        this.clock = clock;
    }

    public List<IncidenciaAdminResponse> incidencias(List<EstadoIncidencia> estados, Long negocioId) {
        Set<EstadoIncidencia> filtro = estados == null || estados.isEmpty()
                ? EnumSet.allOf(EstadoIncidencia.class)
                : EnumSet.copyOf(estados);
        return incidencias.buscar(filtro, negocioId, Limit.unlimited()).stream()
                .map(IncidenciaAdminResponse::de)
                .toList();
    }

    public ResumenAdminResponse resumen() {
        OffsetDateTime inicioHoy = Hoy.inicio(clock);
        LocalDateTime ahora = Hoy.ahoraLocal(clock);

        Map<Long, Long> pedidosPorNegocio = pedidos.contarPorNegocioDesde(inicioHoy).stream()
                .collect(Collectors.toMap(PedidoRepository.PedidosPorNegocio::getNegocioId,
                        PedidoRepository.PedidosPorNegocio::getPedidos));
        List<Negocio> activos = negocios.findByActivoOrderByNombreAsc(true);
        List<PedidosNegocioHoy> porNegocio = activos.stream()
                .map(n -> new PedidosNegocioHoy(n.getId(), n.getNombre(), n.getLocalidad().getNombre(),
                        pedidosPorNegocio.getOrDefault(n.getId(), 0L), n.estaAbierto(ahora)))
                .sorted(Comparator.comparingLong(PedidosNegocioHoy::pedidos).reversed()
                        .thenComparing(PedidosNegocioHoy::nombre))
                .toList();

        return new ResumenAdminResponse(
                activos.size(),
                negocios.count(),
                pedidos.countByFechaCreacionGreaterThanEqual(inicioHoy),
                pedidos.contarLocalidadesConPedidosDesde(inicioHoy),
                incidencias.countByEstadoIn(SIN_RESOLVER),
                incidencias.countByEstadoInAndFechaCreacionGreaterThanEqual(SIN_RESOLVER, inicioHoy),
                repartos.contarRepartidoresEnRuta(),
                porNegocio,
                incidencias.buscar(SIN_RESOLVER, null, Limit.of(INCIDENCIAS_RECIENTES)).stream()
                        .map(IncidenciaAdminResponse::de)
                        .toList());
    }
}
