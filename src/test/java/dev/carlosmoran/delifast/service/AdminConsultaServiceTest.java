package dev.carlosmoran.delifast.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.carlosmoran.delifast.domain.EstadoIncidencia;
import dev.carlosmoran.delifast.domain.Localidad;
import dev.carlosmoran.delifast.domain.Negocio;
import dev.carlosmoran.delifast.dto.admin.ResumenAdminResponse;
import dev.carlosmoran.delifast.repository.IncidenciaRepository;
import dev.carlosmoran.delifast.repository.NegocioRepository;
import dev.carlosmoran.delifast.repository.PedidoRepository;
import dev.carlosmoran.delifast.repository.RepartoRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Limit;
import org.springframework.test.util.ReflectionTestUtils;

class AdminConsultaServiceTest {

    private final NegocioRepository negocios = mock(NegocioRepository.class);
    private final PedidoRepository pedidos = mock(PedidoRepository.class);
    private final IncidenciaRepository incidencias = mock(IncidenciaRepository.class);
    private final RepartoRepository repartos = mock(RepartoRepository.class);
    // 16-11-2026 00:30 en Madrid = 15-11-2026 23:30 UTC: "hoy" ya es el día 16.
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-15T23:30:00Z"), ZoneId.of("UTC"));
    private final AdminConsultaService servicio = new AdminConsultaService(negocios, pedidos, incidencias, repartos, clock);

    private static Negocio negocio(long id, String nombre) {
        Negocio n = new Negocio(new Localidad("Osuna", "Sevilla"), nombre, "955000000", "C/ A");
        ReflectionTestUtils.setField(n, "id", id);
        return n;
    }

    private static PedidoRepository.PedidosPorNegocio cuenta(long negocioId, long pedidos) {
        return new PedidoRepository.PedidosPorNegocio() {
            public Long getNegocioId() { return negocioId; }
            public long getPedidos() { return pedidos; }
        };
    }

    @Test
    void sinFiltroDeEstadoBuscaTodos() {
        servicio.incidencias(null, 7L);

        verify(incidencias).buscar(EnumSet.allOf(EstadoIncidencia.class), 7L, Limit.unlimited());
    }

    @Test
    void resumenCuentaDesdeLaMedianocheDeMadridYOrdenaPorPedidos() {
        OffsetDateTime medianoche = OffsetDateTime.parse("2026-11-16T00:00:00+01:00");
        when(negocios.findByActivoOrderByNombreAsc(true)).thenReturn(List.of(negocio(1, "Asador"), negocio(2, "Burger")));
        when(negocios.count()).thenReturn(3L);
        when(pedidos.contarPorNegocioDesde(any())).thenReturn(List.of(cuenta(2, 5), cuenta(3, 1)));
        when(pedidos.countByFechaCreacionGreaterThanEqual(any())).thenReturn(6L);
        when(incidencias.buscar(any(), isNull(), eq(Limit.of(5)))).thenReturn(List.of());

        ResumenAdminResponse r = servicio.resumen();

        verify(pedidos).contarPorNegocioDesde(eq(medianoche));
        assertThat(r.negociosActivos()).isEqualTo(2);
        assertThat(r.negociosTotales()).isEqualTo(3);
        assertThat(r.pedidosHoy()).isEqualTo(6);
        assertThat(r.pedidosHoyPorNegocio()).extracting(ResumenAdminResponse.PedidosNegocioHoy::nombre)
                .containsExactly("Burger", "Asador");
        assertThat(r.pedidosHoyPorNegocio().get(1).pedidos()).isZero();
        verify(incidencias).countByEstadoIn(EnumSet.of(EstadoIncidencia.ABIERTA, EstadoIncidencia.RESPONDIDA));
    }
}
