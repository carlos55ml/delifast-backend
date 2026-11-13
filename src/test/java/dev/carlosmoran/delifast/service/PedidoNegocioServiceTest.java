package dev.carlosmoran.delifast.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.carlosmoran.delifast.domain.Cliente;
import dev.carlosmoran.delifast.domain.EstadoPedido;
import dev.carlosmoran.delifast.domain.GestorNegocio;
import dev.carlosmoran.delifast.domain.Localidad;
import dev.carlosmoran.delifast.domain.MetodoPago;
import dev.carlosmoran.delifast.domain.Negocio;
import dev.carlosmoran.delifast.domain.Pedido;
import dev.carlosmoran.delifast.domain.Repartidor;
import dev.carlosmoran.delifast.domain.Rol;
import dev.carlosmoran.delifast.dto.negocio.PedidoNegocioResponse;
import dev.carlosmoran.delifast.exception.ConflictoException;
import dev.carlosmoran.delifast.exception.RecursoNoEncontradoException;
import dev.carlosmoran.delifast.exception.ReglaNegocioException;
import dev.carlosmoran.delifast.repository.NegocioRepository;
import dev.carlosmoran.delifast.repository.PedidoRepository;
import dev.carlosmoran.delifast.repository.RepartidorRepository;
import dev.carlosmoran.delifast.repository.RepartoRepository;
import dev.carlosmoran.delifast.repository.UsuarioRepository;
import dev.carlosmoran.delifast.security.UsuarioAutenticado;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

class PedidoNegocioServiceTest {

    private static final UUID GESTOR_ID = UUID.randomUUID();
    private static final UUID MARTA_ID = UUID.randomUUID();
    private static final UsuarioAutenticado GESTOR = new UsuarioAutenticado(GESTOR_ID, Rol.NEGOCIO, 7L, true);

    private final NegocioRepository negocios = mock(NegocioRepository.class);
    private final PedidoRepository pedidos = mock(PedidoRepository.class);
    private final RepartidorRepository repartidores = mock(RepartidorRepository.class);
    private final RepartoRepository repartos = mock(RepartoRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final PedidoNegocioService servicio = new PedidoNegocioService(new NegocioActual(negocios), pedidos,
            repartidores, repartos, usuarios);

    private Negocio sirena;
    private Pedido pedido;
    private final GestorNegocio lola = mock(GestorNegocio.class);

    @BeforeEach
    void preparar() {
        sirena = new Negocio(new Localidad("Osuna", "Sevilla"), "La Sirena", "955000000", "C/ A");
        ReflectionTestUtils.setField(sirena, "id", 7L);
        when(negocios.findById(7L)).thenReturn(Optional.of(sirena));
        Cliente antonio = mock(Cliente.class);
        when(antonio.getNombre()).thenReturn("Antonio");
        pedido = new Pedido(antonio, sirena, MetodoPago.EFECTIVO, "Av. Constitución 8", "612345678");
        ReflectionTestUtils.setField(pedido, "id", 43L);
        ReflectionTestUtils.setField(pedido, "codigo", "DF-001043");
        pedido.calcularTotal();
        when(pedidos.bloquear(43L, 7L)).thenReturn(Optional.of(pedido));
        when(lola.getNombre()).thenReturn("Lola");
        when(usuarios.getReferenceById(GESTOR_ID)).thenReturn(lola);
    }

    private void enEstado(EstadoPedido estado) {
        ReflectionTestUtils.setField(pedido, "estado", estado);
    }

    private Repartidor repartidor(Negocio negocio, boolean activo, boolean disponible) {
        Repartidor r = mock(Repartidor.class);
        when(r.getId()).thenReturn(MARTA_ID);
        when(r.getNombre()).thenReturn("Marta");
        when(r.getNegocio()).thenReturn(negocio);
        when(r.isActivo()).thenReturn(activo);
        when(r.isDisponible()).thenReturn(disponible);
        when(repartidores.findById(MARTA_ID)).thenReturn(Optional.of(r));
        return r;
    }

    @Test
    void sinFiltroListaLosCuatroEstadosActivos() {
        when(pedidos.findByNegocioIdAndEstadoInOrderByFechaCreacionAsc(any(), any())).thenReturn(List.of(pedido));

        servicio.listar(GESTOR, List.of());

        verify(pedidos).findByNegocioIdAndEstadoInOrderByFechaCreacionAsc(7L, PedidoNegocioService.ACTIVOS);
    }

    @Test
    void aceptarGuardaTiempoYHistorial() {
        PedidoNegocioResponse r = servicio.aceptar(GESTOR, 43L, 25);

        assertThat(r.estado()).isEqualTo(EstadoPedido.EN_PREPARACION);
        assertThat(r.tiempoEstimadoMin()).isEqualTo(25);
        assertThat(r.historial()).singleElement().satisfies(c -> {
            assertThat(c.estadoAnterior()).isEqualTo(EstadoPedido.RECIBIDO);
            assertThat(c.estadoNuevo()).isEqualTo(EstadoPedido.EN_PREPARACION);
            assertThat(c.autor()).isEqualTo("Lola");
        });
    }

    @Test
    void aceptarUnPedidoYaAceptadoEsConflicto() {
        enEstado(EstadoPedido.EN_PREPARACION);

        assertThatThrownBy(() -> servicio.aceptar(GESTOR, 43L, 25)).isInstanceOf(ConflictoException.class);
        assertThat(pedido.getHistorial()).isEmpty();
    }

    @Test
    void rechazarUnPedidoCanceladoEsConflicto() {
        enEstado(EstadoPedido.CANCELADO);

        assertThatThrownBy(() -> servicio.rechazar(GESTOR, 43L, "Sin pan")).isInstanceOf(ConflictoException.class);
    }

    @Test
    void rechazarGuardaElMotivo() {
        PedidoNegocioResponse r = servicio.rechazar(GESTOR, 43L, " Sin pan ");

        assertThat(r.estado()).isEqualTo(EstadoPedido.RECHAZADO);
        assertThat(r.motivoRechazo()).isEqualTo("Sin pan");
    }

    @Test
    void listoSoloDesdeEnPreparacion() {
        assertThatThrownBy(() -> servicio.marcarListo(GESTOR, 43L)).isInstanceOf(ConflictoException.class);

        enEstado(EstadoPedido.EN_PREPARACION);
        assertThat(servicio.marcarListo(GESTOR, 43L).estado()).isEqualTo(EstadoPedido.LISTO);
    }

    @Test
    void pedidoDeOtroNegocioEsNoEncontrado() {
        when(pedidos.bloquear(44L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.marcarListo(GESTOR, 44L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void asignarCreaElRepartoYPasaAEnReparto() {
        enEstado(EstadoPedido.LISTO);
        repartidor(sirena, true, true);

        PedidoNegocioResponse r = servicio.asignar(GESTOR, 43L, MARTA_ID);

        assertThat(r.estado()).isEqualTo(EstadoPedido.EN_REPARTO);
        assertThat(r.reparto().repartidor().nombre()).isEqualTo("Marta");
        verify(repartos).save(pedido.getReparto());
    }

    @Test
    void asignarUnPedidoNoListoEsConflicto() {
        enEstado(EstadoPedido.EN_REPARTO);

        assertThatThrownBy(() -> servicio.asignar(GESTOR, 43L, MARTA_ID)).isInstanceOf(ConflictoException.class);
        verify(repartos, never()).save(any());
    }

    @Test
    void repartidorDeOtroNegocioNoEsAsignable() {
        enEstado(EstadoPedido.LISTO);
        Negocio otro = new Negocio(sirena.getLocalidad(), "Otro", "955000001", "C/ B");
        ReflectionTestUtils.setField(otro, "id", 8L);
        repartidor(otro, true, true);

        assertThatThrownBy(() -> servicio.asignar(GESTOR, 43L, MARTA_ID)).isInstanceOf(ReglaNegocioException.class);
        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.LISTO);
    }

    @Test
    void repartidorNoDisponibleODesactivadoNoEsAsignable() {
        enEstado(EstadoPedido.LISTO);
        repartidor(sirena, true, false);
        assertThatThrownBy(() -> servicio.asignar(GESTOR, 43L, MARTA_ID)).hasMessageContaining("disponible");

        repartidor(sirena, false, true);
        assertThatThrownBy(() -> servicio.asignar(GESTOR, 43L, MARTA_ID)).hasMessageContaining("desactivado");
        verify(repartos, never()).save(any());
    }

    @Test
    void repartidorInexistenteNoEsAsignable() {
        enEstado(EstadoPedido.LISTO);
        when(repartidores.findById(MARTA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.asignar(GESTOR, 43L, MARTA_ID)).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void negocioSuspendidoNoPuedeOperar() {
        sirena.setActivo(false);

        assertThatThrownBy(() -> servicio.aceptar(GESTOR, 43L, 25)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void cuentaSinNegocioNoPuedeOperar() {
        UsuarioAutenticado sinNegocio = new UsuarioAutenticado(GESTOR_ID, Rol.NEGOCIO, null, false);

        assertThatThrownBy(() -> servicio.listar(sinNegocio, null)).isInstanceOf(AccessDeniedException.class);
    }
}
