package dev.carlosmoran.delifast.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.carlosmoran.delifast.domain.Cliente;
import dev.carlosmoran.delifast.domain.Direccion;
import dev.carlosmoran.delifast.domain.Localidad;
import dev.carlosmoran.delifast.domain.Rol;
import dev.carlosmoran.delifast.dto.cliente.DatosDireccionRequest;
import dev.carlosmoran.delifast.exception.ValidacionException;
import dev.carlosmoran.delifast.repository.DireccionRepository;
import dev.carlosmoran.delifast.repository.LocalidadRepository;
import dev.carlosmoran.delifast.repository.UsuarioRepository;
import dev.carlosmoran.delifast.security.UsuarioAutenticado;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class DireccionServiceTest {

    private static final UUID ID = UUID.randomUUID();
    private static final UsuarioAutenticado ANTONIO = new UsuarioAutenticado(ID, Rol.CLIENTE, null, false);

    private final DireccionRepository direcciones = mock(DireccionRepository.class);
    private final LocalidadRepository localidades = mock(LocalidadRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final DireccionService servicio = new DireccionService(direcciones, localidades, usuarios);
    private final List<Direccion> guardadas = new ArrayList<>();
    private Cliente antonio;
    private Localidad osuna;

    @BeforeEach
    void preparar() throws Exception {
        Constructor<Cliente> c = Cliente.class.getDeclaredConstructor();
        c.setAccessible(true);
        antonio = c.newInstance();
        ReflectionTestUtils.setField(antonio, "id", ID);
        when(usuarios.findById(ID)).thenReturn(Optional.of(antonio));
        osuna = new Localidad("Osuna", "Sevilla");
        ReflectionTestUtils.setField(osuna, "id", 1L);
        when(localidades.findById(1L)).thenReturn(Optional.of(osuna));
        when(direcciones.findByClienteIdOrderByPredeterminadaDescAliasAsc(ID)).thenReturn(guardadas);
        when(direcciones.save(any())).thenAnswer(inv -> {
            Direccion d = inv.getArgument(0);
            ReflectionTestUtils.setField(d, "id", (long) guardadas.size() + 1);
            guardadas.add(d);
            return d;
        });
    }

    private static DatosDireccionRequest datos(String alias, Boolean predeterminada) {
        return new DatosDireccionRequest(alias, "C/ Mayor 1", "41640", 1L, null, predeterminada);
    }

    @Test
    void laPrimeraEsPredeterminada() {
        assertThat(servicio.crear(ANTONIO, datos("Casa", null)).predeterminada()).isTrue();
    }

    @Test
    void marcarOtraDesmarcaLaAnterior() {
        when(direcciones.existsByClienteId(ID)).thenReturn(false, true);
        servicio.crear(ANTONIO, datos("Casa", null));
        servicio.crear(ANTONIO, datos("Trabajo", true));

        assertThat(guardadas).extracting(Direccion::isPredeterminada).containsExactly(false, true);
    }

    @Test
    void sinMarcarNoCambiaLaPredeterminada() {
        when(direcciones.existsByClienteId(ID)).thenReturn(false, true);
        servicio.crear(ANTONIO, datos("Casa", null));
        servicio.crear(ANTONIO, datos("Trabajo", false));

        assertThat(guardadas).extracting(Direccion::isPredeterminada).containsExactly(true, false);
    }

    @Test
    void localidadInexistente() {
        assertThatThrownBy(() -> servicio.crear(ANTONIO,
                new DatosDireccionRequest("Casa", "C/ Mayor 1", "41640", 99L, null, null)))
                .isInstanceOf(ValidacionException.class);
    }
}
