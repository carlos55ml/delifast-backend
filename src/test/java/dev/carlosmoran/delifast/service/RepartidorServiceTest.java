package dev.carlosmoran.delifast.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.carlosmoran.delifast.domain.Cliente;
import dev.carlosmoran.delifast.domain.EstadoPedido;
import dev.carlosmoran.delifast.domain.Localidad;
import dev.carlosmoran.delifast.domain.MetodoPago;
import dev.carlosmoran.delifast.domain.Negocio;
import dev.carlosmoran.delifast.domain.OrigenIncidencia;
import dev.carlosmoran.delifast.domain.Pedido;
import dev.carlosmoran.delifast.domain.Repartidor;
import dev.carlosmoran.delifast.domain.Reparto;
import dev.carlosmoran.delifast.domain.Rol;
import dev.carlosmoran.delifast.domain.TipoIncidencia;
import dev.carlosmoran.delifast.dto.negocio.IncidenciaNegocioResponse;
import dev.carlosmoran.delifast.dto.repartidor.RepartoRepartidorResponse;
import dev.carlosmoran.delifast.exception.ConflictoException;
import dev.carlosmoran.delifast.exception.RecursoNoEncontradoException;
import dev.carlosmoran.delifast.repository.IncidenciaRepository;
import dev.carlosmoran.delifast.repository.RepartidorRepository;
import dev.carlosmoran.delifast.repository.RepartoRepository;
import dev.carlosmoran.delifast.security.UsuarioAutenticado;
import java.lang.reflect.Constructor;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

class RepartidorServiceTest {

    private static final UUID MARTA_ID = UUID.randomUUID();
    private static final UsuarioAutenticado MARTA = new UsuarioAutenticado(MARTA_ID, Rol.REPARTIDOR, 7L, false);

    private final RepartidorRepository repartidores = mock(RepartidorRepository.class);
    private final RepartoRepository repartos = mock(RepartoRepository.class);
    private final IncidenciaRepository incidencias = mock(IncidenciaRepository.class);
    private final RepartidorService servicio = new RepartidorService(repartidores, repartos, incidencias,
            Clock.fixed(Instant.parse("2026-11-13T12:00:00Z"), ZoneId.of("UTC")));

    private Negocio sirena;
    private Repartidor marta;
    private Pedido pedido;
    private Reparto reparto;

    @BeforeEach
    void preparar() throws Exception {
        sirena = new Negocio(new Localidad("Osuna", "Sevilla"), "La Sirena", "955000000", "C/ A");
        ReflectionTestUtils.setField(sirena, "id", 7L);
        Constructor<Repartidor> cr = Repartidor.class.getDeclaredConstructor();
        cr.setAccessible(true);
        marta = cr.newInstance();
        ReflectionTestUtils.setField(marta, "id", MARTA_ID);
        ReflectionTestUtils.setField(marta, "negocio", sirena);
        marta.setNombre("Marta");
        when(repartidores.findById(MARTA_ID)).thenReturn(Optional.of(marta));

        Constructor<Cliente> cc = Cliente.class.getDeclaredConstructor();
        cc.setAccessible(true);
        Cliente juan = cc.newInstance();
        juan.setNombre("Juan");
        pedido = new Pedido(juan, sirena, MetodoPago.TARJETA, "C/ Carrera 12, 41640 Osuna", "612345678");
        ReflectionTestUtils.setField(pedido, "codigo", "DF-001050");
        pedido.calcularTotal();
        reparto = pedido.asignarRepartidor(marta, null);
        ReflectionTestUtils.setField(reparto, "id", 30L);
        when(repartos.bloquear(30L, MARTA_ID)).thenReturn(Optional.of(reparto));
        when(repartos.findByIdAndRepartidorId(30L, MARTA_ID)).thenReturn(Optional.of(reparto));
    }

    @Test
    void entregarGuardaCobroYPasaAEntregado() {
        RepartoRepartidorResponse r = servicio.entregar(MARTA, 30L, MetodoPago.EFECTIVO);

        assertThat(r.estado()).isEqualTo(EstadoPedido.ENTREGADO);
        assertThat(r.metodoCobro()).isEqualTo(MetodoPago.EFECTIVO);
        assertThat(r.fechaEntrega()).isNotNull();
        assertThat(reparto.isCobroConfirmado()).isTrue();
        assertThat(pedido.getHistorial().getLast().getEstadoNuevo()).isEqualTo(EstadoPedido.ENTREGADO);
    }

    @Test
    void entregarDosVecesEsConflicto() {
        servicio.entregar(MARTA, 30L, MetodoPago.EFECTIVO);
        assertThatThrownBy(() -> servicio.entregar(MARTA, 30L, MetodoPago.EFECTIVO)).isInstanceOf(ConflictoException.class);
    }

    @Test
    void repartoDeOtroNoExiste() {
        assertThatThrownBy(() -> servicio.entregar(MARTA, 31L, MetodoPago.EFECTIVO))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.reparto(MARTA, 31L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void incidenciaSinDescripcionUsaElTipo() {
        when(incidencias.save(any())).thenAnswer(inv -> inv.getArgument(0));

        IncidenciaNegocioResponse r = servicio.comunicarIncidencia(MARTA, 30L, TipoIncidencia.CLIENTE_AUSENTE, null);

        assertThat(r.origen()).isEqualTo(OrigenIncidencia.REPARTIDOR);
        assertThat(r.descripcion()).isEqualTo("El cliente no está en la dirección");
        assertThat(r.autor().nombre()).isEqualTo("Marta");
        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.EN_REPARTO);
    }

    @Test
    void entregadosSoloDeHoyEnMadrid() {
        OffsetDateTime hoy = OffsetDateTime.parse("2026-11-13T00:00:00+01:00");
        when(repartos.findByRepartidorIdAndPedidoEstadoAndFechaEntregaGreaterThanEqualOrderByFechaAsignacionDesc(
                MARTA_ID, EstadoPedido.ENTREGADO, hoy)).thenReturn(List.of(reparto));

        assertThat(servicio.repartos(MARTA, EstadoPedido.ENTREGADO)).hasSize(1);
    }

    @Test
    void disponibilidadYPerfil() {
        when(repartos.countByRepartidorIdAndPedidoEstado(MARTA_ID, EstadoPedido.EN_REPARTO)).thenReturn(1L);

        var perfil = servicio.cambiarDisponible(MARTA, true);

        assertThat(perfil.disponible()).isTrue();
        assertThat(perfil.enCurso()).isEqualTo(1);
        assertThat(perfil.negocio().localidad()).isEqualTo("Osuna");
    }

    @Test
    void desactivadoONegocioSuspendidoNoOpera() {
        sirena.setActivo(false);
        assertThatThrownBy(() -> servicio.perfil(MARTA)).isInstanceOf(AccessDeniedException.class);
        sirena.setActivo(true);
        marta.setActivo(false);
        assertThatThrownBy(() -> servicio.perfil(MARTA)).isInstanceOf(AccessDeniedException.class);
    }
}
