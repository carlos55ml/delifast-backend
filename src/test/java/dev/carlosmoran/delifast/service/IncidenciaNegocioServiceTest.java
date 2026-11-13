package dev.carlosmoran.delifast.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.carlosmoran.delifast.domain.Cliente;
import dev.carlosmoran.delifast.domain.EstadoIncidencia;
import dev.carlosmoran.delifast.domain.GestorNegocio;
import dev.carlosmoran.delifast.domain.Incidencia;
import dev.carlosmoran.delifast.domain.Localidad;
import dev.carlosmoran.delifast.domain.MetodoPago;
import dev.carlosmoran.delifast.domain.Negocio;
import dev.carlosmoran.delifast.domain.OrigenIncidencia;
import dev.carlosmoran.delifast.domain.Pedido;
import dev.carlosmoran.delifast.domain.Rol;
import dev.carlosmoran.delifast.domain.TipoIncidencia;
import dev.carlosmoran.delifast.dto.negocio.IncidenciaNegocioResponse;
import dev.carlosmoran.delifast.exception.ConflictoException;
import dev.carlosmoran.delifast.exception.RecursoNoEncontradoException;
import dev.carlosmoran.delifast.repository.IncidenciaRepository;
import dev.carlosmoran.delifast.repository.NegocioRepository;
import dev.carlosmoran.delifast.repository.UsuarioRepository;
import dev.carlosmoran.delifast.security.UsuarioAutenticado;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class IncidenciaNegocioServiceTest {

    private static final UsuarioAutenticado GESTOR = new UsuarioAutenticado(UUID.randomUUID(), Rol.NEGOCIO, 7L, false);

    private final NegocioRepository negocios = mock(NegocioRepository.class);
    private final IncidenciaRepository incidencias = mock(IncidenciaRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final IncidenciaNegocioService servicio = new IncidenciaNegocioService(new NegocioActual(negocios),
            incidencias, usuarios);
    private final GestorNegocio ana = mock(GestorNegocio.class);
    private Incidencia retraso;

    @BeforeEach
    void preparar() {
        Negocio n = new Negocio(new Localidad("Osuna", "Sevilla"), "La Sirena", "955000000", "C/ A");
        ReflectionTestUtils.setField(n, "id", 7L);
        when(negocios.findById(7L)).thenReturn(Optional.of(n));
        Cliente juan = mock(Cliente.class);
        when(juan.getNombre()).thenReturn("Juan Torres");
        Pedido pedido = new Pedido(juan, n, MetodoPago.TARJETA, "C/ B", "612345678");
        pedido.calcularTotal();
        retraso = new Incidencia(pedido, juan, OrigenIncidencia.CLIENTE, TipoIncidencia.RETRASO, "Lleva una hora");
        ReflectionTestUtils.setField(retraso, "id", 4L);
        when(incidencias.buscarDeNegocio(4L, 7L)).thenReturn(Optional.of(retraso));
        when(usuarios.getReferenceById(GESTOR.id())).thenReturn(ana);
    }

    @Test
    void sinFiltroBuscaTodas() {
        servicio.listar(GESTOR, null);
        verify(incidencias).buscarDeNegocio(7L, EnumSet.allOf(EstadoIncidencia.class));
    }

    @Test
    void responderLaPasaARespondidaYPuedeCambiarse() {
        servicio.responder(GESTOR, 4L, " Ya sale ");
        IncidenciaNegocioResponse r = servicio.responder(GESTOR, 4L, "Lo sentimos, sale ahora");

        assertThat(r.estado()).isEqualTo(EstadoIncidencia.RESPONDIDA);
        assertThat(r.respuesta()).isEqualTo("Lo sentimos, sale ahora");
        assertThat(r.autor().nombre()).isEqualTo("Juan Torres");
        assertThat(r.pedido().cliente().telefono()).isEqualTo("612345678");
        assertThat(retraso.getResponsable()).isSameAs(ana);
    }

    @Test
    void resolverConRespuestaGuardaAmbasYLaFecha() {
        IncidenciaNegocioResponse r = servicio.resolver(GESTOR, 4L, "Te devolvemos el envío");

        assertThat(r.estado()).isEqualTo(EstadoIncidencia.RESUELTA);
        assertThat(r.respuesta()).isEqualTo("Te devolvemos el envío");
        assertThat(r.fechaResolucion()).isNotNull();
    }

    @Test
    void resolverSinRespuestaConservaLaAnterior() {
        servicio.responder(GESTOR, 4L, "Ya sale");
        assertThat(servicio.resolver(GESTOR, 4L, null).respuesta()).isEqualTo("Ya sale");
    }

    @Test
    void unaResueltaNoSeTocaMas() {
        servicio.resolver(GESTOR, 4L, null);

        assertThatThrownBy(() -> servicio.responder(GESTOR, 4L, "x")).isInstanceOf(ConflictoException.class);
        assertThatThrownBy(() -> servicio.resolver(GESTOR, 4L, null)).isInstanceOf(ConflictoException.class);
    }

    @Test
    void deOtroNegocioNoExiste() {
        assertThatThrownBy(() -> servicio.responder(GESTOR, 99L, "x")).isInstanceOf(RecursoNoEncontradoException.class);
    }
}
