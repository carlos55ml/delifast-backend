package dev.carlosmoran.delifast.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import dev.carlosmoran.delifast.domain.GestorNegocio;
import dev.carlosmoran.delifast.domain.Localidad;
import dev.carlosmoran.delifast.domain.Negocio;
import dev.carlosmoran.delifast.domain.Repartidor;
import dev.carlosmoran.delifast.domain.Rol;
import dev.carlosmoran.delifast.dto.negocio.AltaMiembroRequest;
import dev.carlosmoran.delifast.dto.negocio.DatosMiembroRequest;
import dev.carlosmoran.delifast.dto.negocio.EmpleadoNegocioResponse;
import dev.carlosmoran.delifast.dto.negocio.RepartidorNegocioResponse;
import dev.carlosmoran.delifast.exception.ConflictoException;
import dev.carlosmoran.delifast.exception.RecursoNoEncontradoException;
import dev.carlosmoran.delifast.exception.ReglaNegocioException;
import dev.carlosmoran.delifast.repository.GestorNegocioRepository;
import dev.carlosmoran.delifast.repository.NegocioRepository;
import dev.carlosmoran.delifast.repository.RepartidorRepository;
import dev.carlosmoran.delifast.repository.RepartoRepository;
import dev.carlosmoran.delifast.repository.UsuarioRepository;
import dev.carlosmoran.delifast.security.UsuarioAutenticado;
import dev.carlosmoran.delifast.supabase.NuevaCuenta;
import dev.carlosmoran.delifast.supabase.SupabaseAuthAdmin;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

class EquipoServiceTest {

    private static final UUID LOLA_ID = UUID.randomUUID();
    private static final UUID NUEVO_ID = UUID.randomUUID();
    private static final UsuarioAutenticado DUENO = new UsuarioAutenticado(LOLA_ID, Rol.NEGOCIO, 7L, true);
    private static final UsuarioAutenticado EMPLEADO = new UsuarioAutenticado(UUID.randomUUID(), Rol.NEGOCIO, 7L, false);
    private static final AltaMiembroRequest ANA = new AltaMiembroRequest(" Ana Moreno ", " ", "Ana@LaSirena.es");

    private final NegocioRepository negocios = mock(NegocioRepository.class);
    private final GestorNegocioRepository gestores = mock(GestorNegocioRepository.class);
    private final RepartidorRepository repartidores = mock(RepartidorRepository.class);
    private final RepartoRepository repartos = mock(RepartoRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final SupabaseAuthAdmin authAdmin = mock(SupabaseAuthAdmin.class);
    private final EquipoService servicio = new EquipoService(new NegocioActual(negocios), gestores, repartidores,
            repartos, usuarios, authAdmin, Clock.fixed(Instant.parse("2026-11-16T10:00:00Z"), ZoneId.of("UTC")));

    private Negocio sirena;

    @BeforeEach
    void preparar() {
        sirena = new Negocio(new Localidad("Osuna", "Sevilla"), "La Sirena", "955000000", "C/ A");
        ReflectionTestUtils.setField(sirena, "id", 7L);
        when(negocios.findById(7L)).thenReturn(Optional.of(sirena));
    }

    private static <T> T cuenta(Class<T> tipo, UUID id, String nombre, boolean esDueno) {
        try {
            var constructor = tipo.getDeclaredConstructor();
            constructor.setAccessible(true);
            T c = constructor.newInstance();
            ReflectionTestUtils.setField(c, "id", id);
            ReflectionTestUtils.setField(c, "nombre", nombre);
            if (c instanceof GestorNegocio) {
                ReflectionTestUtils.setField(c, "esDueno", esDueno);
            }
            return c;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void elDuenoPrimeroYLuegoPorNombre() {
        when(gestores.findByNegocioId(7L)).thenReturn(List.of(
                cuenta(GestorNegocio.class, UUID.randomUUID(), "Zoe", false),
                cuenta(GestorNegocio.class, LOLA_ID, "Lola", true),
                cuenta(GestorNegocio.class, UUID.randomUUID(), "ana", false)));

        assertThat(servicio.empleados(DUENO)).extracting(EmpleadoNegocioResponse::nombre)
                .containsExactly("Lola", "ana", "Zoe");
    }

    @Test
    void altaDeEmpleadoInvitaConRolNegocioSinSerDueno() {
        when(authAdmin.invitar(any())).thenReturn(NUEVO_ID);

        EmpleadoNegocioResponse r = servicio.altaEmpleado(DUENO, ANA);

        verify(authAdmin).invitar(new NuevaCuenta("ana@lasirena.es", "Ana Moreno", null, Rol.NEGOCIO, 7L, false));
        assertThat(r).isEqualTo(new EmpleadoNegocioResponse(NUEVO_ID, "Ana Moreno", "ana@lasirena.es", null, false, true));
    }

    @Test
    void altaDeRepartidorInvitaConRolRepartidor() {
        when(authAdmin.invitar(any())).thenReturn(NUEVO_ID);

        RepartidorNegocioResponse r = servicio.altaRepartidor(DUENO,
                new AltaMiembroRequest("Marta", "655333444", "marta@lasirena.es"));

        verify(authAdmin).invitar(new NuevaCuenta("marta@lasirena.es", "Marta", "655333444", Rol.REPARTIDOR, 7L, false));
        assertThat(r.activo()).isTrue();
        assertThat(r.disponible()).isFalse();
    }

    @Test
    void soloElDuenoDaDeAlta() {
        assertThatThrownBy(() -> servicio.altaRepartidor(EMPLEADO, ANA)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(authAdmin);
    }

    @Test
    void emailRepetidoEsConflicto() {
        when(usuarios.existsByEmailIgnoreCase("ana@lasirena.es")).thenReturn(true);

        assertThatThrownBy(() -> servicio.altaEmpleado(DUENO, ANA)).isInstanceOf(ConflictoException.class);
        verifyNoInteractions(authAdmin);
    }

    @Test
    void noSePuedeDesactivarAlDueno() {
        when(gestores.findByIdAndNegocioId(LOLA_ID, 7L)).thenReturn(Optional.of(cuenta(GestorNegocio.class, LOLA_ID, "Lola", true)));

        assertThatThrownBy(() -> servicio.cambiarActivoEmpleado(EMPLEADO, LOLA_ID, false))
                .isInstanceOf(ReglaNegocioException.class);
        verify(authAdmin, never()).bloquear(any(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void desactivarEmpleadoBloqueaSuCuentaYReactivarLaDesbloquea() {
        UUID id = UUID.randomUUID();
        GestorNegocio ana = cuenta(GestorNegocio.class, id, "Ana", false);
        when(gestores.findByIdAndNegocioId(id, 7L)).thenReturn(Optional.of(ana));

        assertThat(servicio.cambiarActivoEmpleado(DUENO, id, false).activo()).isFalse();
        verify(authAdmin).bloquear(id, true);

        servicio.cambiarActivoEmpleado(DUENO, id, true);
        verify(authAdmin).bloquear(id, false);
    }

    @Test
    void desactivarRepartidor() {
        UUID id = UUID.randomUUID();
        Repartidor marta = cuenta(Repartidor.class, id, "Marta", false);
        ReflectionTestUtils.setField(marta, "negocio", sirena);
        when(repartidores.findByIdAndNegocioId(id, 7L)).thenReturn(Optional.of(marta));

        assertThat(servicio.cambiarActivoRepartidor(DUENO, id, false).activo()).isFalse();
        verify(authAdmin).bloquear(id, true);
    }

    @Test
    void editarRepartidorDeOtroNegocioNoExiste() {
        assertThatThrownBy(() -> servicio.editarRepartidor(DUENO, UUID.randomUUID(), new DatosMiembroRequest("X", null)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void repartidoresConContadores() {
        UUID id = UUID.randomUUID();
        when(repartidores.findByNegocioIdOrderByNombreAsc(7L)).thenReturn(List.of(cuenta(Repartidor.class, id, "Marta", false)));
        when(repartos.contarEnRepartoPorRepartidor(7L)).thenReturn(List.of(fila(id, 1)));
        when(repartos.contarAsignadosDesde(any(), any())).thenReturn(List.of(fila(id, 4)));

        RepartidorNegocioResponse r = servicio.repartidores(DUENO, null).getFirst();

        assertThat(r.pedidosEnReparto()).isEqualTo(1);
        assertThat(r.repartosHoy()).isEqualTo(4);
    }

    private static RepartoRepository.RepartosPorRepartidor fila(UUID id, long n) {
        return new RepartoRepository.RepartosPorRepartidor() {
            public UUID getRepartidorId() { return id; }
            public long getRepartos() { return n; }
        };
    }
}
