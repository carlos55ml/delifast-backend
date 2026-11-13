package dev.carlosmoran.delifast.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.carlosmoran.delifast.domain.EstadoIncidencia;
import dev.carlosmoran.delifast.domain.Localidad;
import dev.carlosmoran.delifast.domain.Negocio;
import dev.carlosmoran.delifast.domain.Rol;
import dev.carlosmoran.delifast.dto.negocio.EstadisticasNegocioResponse;
import dev.carlosmoran.delifast.dto.negocio.EstadisticasNegocioResponse.Tramo;
import dev.carlosmoran.delifast.dto.negocio.Periodo;
import dev.carlosmoran.delifast.repository.IncidenciaRepository;
import dev.carlosmoran.delifast.repository.NegocioRepository;
import dev.carlosmoran.delifast.repository.PedidoRepository;
import dev.carlosmoran.delifast.security.UsuarioAutenticado;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class EstadisticasServiceTest {

    private static final UsuarioAutenticado GESTOR = new UsuarioAutenticado(UUID.randomUUID(), Rol.NEGOCIO, 7L, true);

    private final NegocioRepository negocios = mock(NegocioRepository.class);
    private final PedidoRepository pedidos = mock(PedidoRepository.class);
    private final IncidenciaRepository incidencias = mock(IncidenciaRepository.class);
    // Jueves 12-11-2026 a las 23:30 UTC = viernes 13-11 a las 00:30 en Madrid.
    private final EstadisticasService servicio = new EstadisticasService(new NegocioActual(negocios), pedidos,
            incidencias, Clock.fixed(Instant.parse("2026-11-12T23:30:00Z"), ZoneId.of("UTC")));

    private void negocio() {
        Negocio n = new Negocio(new Localidad("Osuna", "Sevilla"), "La Sirena", "955000000", "C/ A");
        ReflectionTestUtils.setField(n, "id", 7L);
        when(negocios.findById(7L)).thenReturn(Optional.of(n));
    }

    @Test
    void rangos() {
        LocalDate viernes = LocalDate.of(2026, 11, 13);
        assertThat(EstadisticasService.Rango.de(Periodo.SEMANA, viernes))
                .isEqualTo(new EstadisticasService.Rango(LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 15)));
        assertThat(EstadisticasService.Rango.de(Periodo.MES, viernes).anterior(Periodo.MES))
                .isEqualTo(new EstadisticasService.Rango(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)));
        assertThat(EstadisticasService.Rango.de(Periodo.DIA, LocalDate.of(2026, 3, 1)).anterior(Periodo.DIA).desde())
                .isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(EstadisticasService.Rango.de(Periodo.DIA, viernes).inicio())
                .isEqualTo(OffsetDateTime.parse("2026-11-13T00:00:00+01:00"));
    }

    @Test
    void serieEnHoraDeMadridConCeros() {
        EstadisticasService.Rango dia = EstadisticasService.Rango.de(Periodo.DIA, LocalDate.of(2026, 11, 13));
        List<Tramo> serie = EstadisticasService.serie(Periodo.DIA, dia, List.of(
                OffsetDateTime.parse("2026-11-13T12:10:00Z"), OffsetDateTime.parse("2026-11-13T12:50:00Z")));

        assertThat(serie).hasSize(24);
        assertThat(serie.get(13)).isEqualTo(new Tramo("13:00", 2));
        assertThat(serie.get(12).pedidos()).isZero();
    }

    @Test
    void serieSemanalYMensual() {
        EstadisticasService.Rango semana = EstadisticasService.Rango.de(Periodo.SEMANA, LocalDate.of(2026, 11, 13));
        assertThat(EstadisticasService.serie(Periodo.SEMANA, semana, List.of(OffsetDateTime.parse("2026-11-13T10:00:00Z"))))
                .extracting(Tramo::etiqueta).containsExactly("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom");
        EstadisticasService.Rango febrero = EstadisticasService.Rango.de(Periodo.MES, LocalDate.of(2026, 2, 10));
        assertThat(EstadisticasService.serie(Periodo.MES, febrero, List.of())).hasSize(28).last()
                .isEqualTo(new Tramo("28", 0));
    }

    @Test
    void calculaConElPeriodoAnteriorYHoyEnMadrid() {
        negocio();
        OffsetDateTime hoy = OffsetDateTime.parse("2026-11-13T00:00:00+01:00");
        OffsetDateTime ayer = OffsetDateTime.parse("2026-11-12T00:00:00+01:00");
        when(pedidos.fechasDePedidos(7L, hoy, hoy.plusDays(1)))
                .thenReturn(List.of(OffsetDateTime.parse("2026-11-13T12:00:00Z")));
        when(pedidos.fechasDePedidos(7L, ayer, hoy)).thenReturn(List.of(ayer, ayer, ayer));
        when(pedidos.facturacion(7L, hoy, hoy.plusDays(1))).thenReturn(new BigDecimal("18.5"));
        when(pedidos.facturacion(7L, ayer, hoy)).thenReturn(BigDecimal.ZERO);
        when(pedidos.minutosMediosEntrega(eq(7L), eq(hoy), any())).thenReturn(32.6);
        // Sin entregas el día anterior: la consulta devuelve null (Mockito daría 0.0).
        when(pedidos.minutosMediosEntrega(7L, ayer, hoy)).thenReturn(null);
        when(incidencias.contarPorEstado(7L, hoy, hoy.plusDays(1))).thenReturn(List.of(
                fila(EstadoIncidencia.ABIERTA, 1), fila(EstadoIncidencia.RESPONDIDA, 1), fila(EstadoIncidencia.RESUELTA, 2)));
        when(pedidos.masVendidos(eq(7L), any(), any(), any())).thenReturn(List.of());

        EstadisticasNegocioResponse r = servicio.calcular(GESTOR, Periodo.DIA, null);

        assertThat(r.desde()).isEqualTo(LocalDate.of(2026, 11, 13));
        assertThat(r.pedidos()).isEqualTo(1);
        assertThat(r.pedidosAnterior()).isEqualTo(3);
        assertThat(r.facturacion()).isEqualByComparingTo("18.50");
        assertThat(r.facturacion().scale()).isEqualTo(2);
        assertThat(r.tiempoMedioEntregaMin()).isEqualTo(33);
        assertThat(r.tiempoMedioEntregaAnteriorMin()).isNull();
        assertThat(r.incidencias()).isEqualTo(new EstadisticasNegocioResponse.Incidencias(4, 2, 2));
    }

    private static IncidenciaRepository.IncidenciasPorEstado fila(EstadoIncidencia estado, long total) {
        return new IncidenciaRepository.IncidenciasPorEstado() {
            public EstadoIncidencia getEstado() { return estado; }
            public long getTotal() { return total; }
        };
    }
}
