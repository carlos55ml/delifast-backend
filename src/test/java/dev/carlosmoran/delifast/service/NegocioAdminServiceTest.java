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
import dev.carlosmoran.delifast.dto.admin.AltaNegocioRequest;
import dev.carlosmoran.delifast.dto.admin.NegocioAdminResponse;
import dev.carlosmoran.delifast.exception.ConflictoException;
import dev.carlosmoran.delifast.exception.ReglaNegocioException;
import dev.carlosmoran.delifast.repository.GestorNegocioRepository;
import dev.carlosmoran.delifast.repository.LocalidadRepository;
import dev.carlosmoran.delifast.repository.NegocioRepository;
import dev.carlosmoran.delifast.repository.PedidoRepository;
import dev.carlosmoran.delifast.repository.RepartidorRepository;
import dev.carlosmoran.delifast.repository.UsuarioRepository;
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
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class NegocioAdminServiceTest {

    private static final UUID DUENO_ID = UUID.fromString("00000000-0000-0000-0000-000000000007");

    private final NegocioRepository negocios = mock(NegocioRepository.class);
    private final LocalidadRepository localidades = mock(LocalidadRepository.class);
    private final GestorNegocioRepository gestores = mock(GestorNegocioRepository.class);
    private final RepartidorRepository repartidores = mock(RepartidorRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final PedidoRepository pedidos = mock(PedidoRepository.class);
    private final SupabaseAuthAdmin authAdmin = mock(SupabaseAuthAdmin.class);

    private NegocioAdminService servicio;
    private final Localidad osuna = new Localidad("Osuna", "Sevilla");

    private final AltaNegocioRequest alta = new AltaNegocioRequest(" Heladería La Sirena ", 1L, "C/ San Pedro 5",
            "955 821 456", new AltaNegocioRequest.Dueno("Lola Sánchez", "Lola@LaSirena.es "));

    @BeforeEach
    void preparar() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-16T20:00:00Z"), ZoneId.of("UTC"));
        TransactionTemplate tx = new TransactionTemplate(mock(PlatformTransactionManager.class));
        servicio = new NegocioAdminService(negocios, localidades, gestores, repartidores, usuarios, pedidos,
                authAdmin, tx, clock);
        ReflectionTestUtils.setField(osuna, "id", 1L);
        when(localidades.findById(1L)).thenReturn(Optional.of(osuna));
    }

    private void guardadoConId(long id) {
        when(negocios.save(any(Negocio.class))).thenAnswer(inv -> {
            Negocio n = inv.getArgument(0);
            ReflectionTestUtils.setField(n, "id", id);
            when(negocios.findById(id)).thenReturn(Optional.of(n));
            return n;
        });
    }

    @Test
    void altaCreaElNegocioEInvitaAlDueno() {
        guardadoConId(7L);
        when(authAdmin.invitar(any())).thenReturn(DUENO_ID);

        NegocioAdminResponse creado = servicio.alta(alta);

        ArgumentCaptor<NuevaCuenta> cuenta = ArgumentCaptor.forClass(NuevaCuenta.class);
        verify(authAdmin).invitar(cuenta.capture());
        assertThat(cuenta.getValue()).isEqualTo(new NuevaCuenta("lola@lasirena.es", "Lola Sánchez", Rol.NEGOCIO, 7L, true));
        assertThat(creado.id()).isEqualTo(7L);
        assertThat(creado.nombre()).isEqualTo("Heladería La Sirena");
        assertThat(creado.dueno()).isEqualTo(new NegocioAdminResponse.Dueno(DUENO_ID, "Lola Sánchez", "lola@lasirena.es"));
        assertThat(creado.activo()).isTrue();
        assertThat(creado.abierto()).isFalse();
    }

    @Test
    void emailYaRegistradoEnPerfilesNoCreaNada() {
        when(usuarios.existsByEmailIgnoreCase("lola@lasirena.es")).thenReturn(true);

        assertThatThrownBy(() -> servicio.alta(alta)).isInstanceOf(ConflictoException.class);
        verify(negocios, never()).save(any());
        verifyNoInteractions(authAdmin);
    }

    @Test
    void siFallaLaInvitacionSeBorraElNegocio() {
        guardadoConId(7L);
        when(authAdmin.invitar(any())).thenThrow(new ConflictoException("Ya existe una cuenta con ese email"));

        assertThatThrownBy(() -> servicio.alta(alta)).isInstanceOf(ConflictoException.class);
        verify(negocios).deleteById(7L);
    }

    @Test
    void localidadInexistenteEsReglaDeNegocio() {
        AltaNegocioRequest otra = new AltaNegocioRequest("Bar", 99L, "C/ A", "955000000", alta.dueno());

        assertThatThrownBy(() -> servicio.alta(otra)).isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(authAdmin);
    }

    @Test
    void suspenderBloqueaTodasLasCuentasDelNegocio() {
        Negocio n = new Negocio(osuna, "La Sirena", "955000000", "C/ A");
        ReflectionTestUtils.setField(n, "id", 7L);
        when(negocios.findById(7L)).thenReturn(Optional.of(n));
        GestorNegocio dueno = mock(GestorNegocio.class);
        when(dueno.getId()).thenReturn(DUENO_ID);
        Repartidor repartidor = mock(Repartidor.class);
        UUID repartidorId = UUID.randomUUID();
        when(repartidor.getId()).thenReturn(repartidorId);
        when(gestores.findByNegocioId(7L)).thenReturn(List.of(dueno));
        when(repartidores.findByNegocioId(7L)).thenReturn(List.of(repartidor));

        NegocioAdminResponse r = servicio.cambiarActivo(7L, false);

        assertThat(r.activo()).isFalse();
        verify(authAdmin).bloquear(DUENO_ID, true);
        verify(authAdmin).bloquear(repartidorId, true);
    }

    @Test
    void siYaEstabaActivoNoLlamaASupabase() {
        Negocio n = new Negocio(osuna, "La Sirena", "955000000", "C/ A");
        ReflectionTestUtils.setField(n, "id", 7L);
        when(negocios.findById(7L)).thenReturn(Optional.of(n));

        servicio.cambiarActivo(7L, true);

        verifyNoInteractions(authAdmin);
    }
}
