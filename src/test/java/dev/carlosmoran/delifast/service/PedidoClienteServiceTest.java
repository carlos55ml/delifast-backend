package dev.carlosmoran.delifast.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.carlosmoran.delifast.domain.Categoria;
import dev.carlosmoran.delifast.domain.Cliente;
import dev.carlosmoran.delifast.domain.Direccion;
import dev.carlosmoran.delifast.domain.EstadoPedido;
import dev.carlosmoran.delifast.domain.Localidad;
import dev.carlosmoran.delifast.domain.MetodoPago;
import dev.carlosmoran.delifast.domain.Negocio;
import dev.carlosmoran.delifast.domain.Producto;
import dev.carlosmoran.delifast.domain.Rol;
import dev.carlosmoran.delifast.dto.cliente.NuevoPedidoRequest;
import dev.carlosmoran.delifast.dto.cliente.NuevoPedidoRequest.Linea;
import dev.carlosmoran.delifast.dto.cliente.PedidoClienteResponse;
import dev.carlosmoran.delifast.exception.RecursoNoEncontradoException;
import dev.carlosmoran.delifast.exception.ReglaNegocioException;
import dev.carlosmoran.delifast.repository.DireccionRepository;
import dev.carlosmoran.delifast.repository.NegocioRepository;
import dev.carlosmoran.delifast.repository.PedidoRepository;
import dev.carlosmoran.delifast.repository.ProductoRepository;
import dev.carlosmoran.delifast.repository.UsuarioRepository;
import dev.carlosmoran.delifast.security.UsuarioAutenticado;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PedidoClienteServiceTest {

    private static final UUID ANTONIO_ID = UUID.randomUUID();
    private static final UsuarioAutenticado ANTONIO = new UsuarioAutenticado(ANTONIO_ID, Rol.CLIENTE, null, false);

    private final PedidoRepository pedidos = mock(PedidoRepository.class);
    private final NegocioRepository negocios = mock(NegocioRepository.class);
    private final ProductoRepository productos = mock(ProductoRepository.class);
    private final DireccionRepository direcciones = mock(DireccionRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final PedidoClienteService servicio = new PedidoClienteService(pedidos, negocios, productos, direcciones,
            usuarios, Clock.fixed(Instant.parse("2026-11-13T12:00:00Z"), ZoneId.of("UTC")));

    private Localidad osuna;
    private Negocio sirena;
    private Cliente antonio;
    private Producto clasica;
    private Producto bbq;

    @BeforeEach
    void preparar() throws Exception {
        osuna = new Localidad("Osuna", "Sevilla");
        ReflectionTestUtils.setField(osuna, "id", 1L);
        sirena = new Negocio(osuna, "La Sirena", "955000000", "C/ A");
        ReflectionTestUtils.setField(sirena, "id", 7L);
        sirena.setAbiertoManual(true);
        sirena.setGastosEnvio(new BigDecimal("1.50"));
        sirena.setPedidoMinimo(new BigDecimal("10.00"));
        when(negocios.findByIdAndActivoTrue(7L)).thenReturn(Optional.of(sirena));

        Constructor<Cliente> c = Cliente.class.getDeclaredConstructor();
        c.setAccessible(true);
        antonio = c.newInstance();
        ReflectionTestUtils.setField(antonio, "id", ANTONIO_ID);
        antonio.setNombre("Antonio");
        antonio.setTelefono("612345678");
        when(usuarios.findById(ANTONIO_ID)).thenReturn(Optional.of(antonio));

        Direccion casa = new Direccion(antonio, osuna, "Casa", "Av. Constitución 8", "41640");
        casa.setIndicaciones("Timbre 1ºA");
        when(direcciones.findByIdAndClienteId(3L, ANTONIO_ID)).thenReturn(Optional.of(casa));

        Categoria hamburguesas = new Categoria(sirena, "Hamburguesas", 0);
        clasica = producto(hamburguesas, 5L, "Clásica", "6.50");
        bbq = producto(hamburguesas, 6L, "BBQ", "8.00");
        when(productos.findAllById(any())).thenReturn(List.of(clasica, bbq));
        when(pedidos.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private static Producto producto(Categoria categoria, long id, String nombre, String precio) {
        Producto p = new Producto(categoria, nombre, new BigDecimal(precio));
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }

    private static NuevoPedidoRequest pedido(Linea... lineas) {
        return new NuevoPedidoRequest(7L, 3L, MetodoPago.EFECTIVO, " Sin cebolla ", List.of(lineas));
    }

    @Test
    void calculaPreciosYCopiaDireccionYTelefono() {
        PedidoClienteResponse r = servicio.crear(ANTONIO, pedido(new Linea(5L, 1), new Linea(6L, 1), new Linea(5L, 1)));

        assertThat(r.estado()).isEqualTo(EstadoPedido.RECIBIDO);
        assertThat(r.lineas()).extracting(PedidoClienteResponse.Linea::cantidad).containsExactly(2, 1);
        assertThat(r.subtotal()).isEqualByComparingTo("21.00");
        assertThat(r.gastosEnvio()).isEqualByComparingTo("1.50");
        assertThat(r.total()).isEqualByComparingTo("22.50");
        assertThat(r.direccionEntrega()).isEqualTo("Av. Constitución 8, 41640 Osuna (Timbre 1ºA)");
        assertThat(r.nota()).isEqualTo("Sin cebolla");
        assertThat(r.historial()).singleElement().extracting(PedidoClienteResponse.Cambio::estado)
                .isEqualTo(EstadoPedido.RECIBIDO);
    }

    @Test
    void negocioCerrado() {
        sirena.setAbiertoManual(false);
        assertThatThrownBy(() -> servicio.crear(ANTONIO, pedido(new Linea(5L, 2)))).hasMessageContaining("cerrado");
        verify(pedidos, never()).saveAndFlush(any());
    }

    @Test
    void porDebajoDelMinimo() {
        assertThatThrownBy(() -> servicio.crear(ANTONIO, pedido(new Linea(5L, 1))))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("mínimo");
    }

    @Test
    void productoAgotado() {
        bbq.marcarAgotado();
        assertThatThrownBy(() -> servicio.crear(ANTONIO, pedido(new Linea(6L, 2)))).hasMessageContaining("agotado");
    }

    @Test
    void productoDeOtroNegocioOBorrado() {
        Negocio otro = new Negocio(osuna, "Otro", "955000001", "C/ B");
        ReflectionTestUtils.setField(otro, "id", 8L);
        Producto ajeno = producto(new Categoria(otro, "Pizzas", 0), 9L, "Pizza", "9.00");
        when(productos.findAllById(any())).thenReturn(List.of(ajeno));
        assertThatThrownBy(() -> servicio.crear(ANTONIO, pedido(new Linea(9L, 2)))).hasMessageContaining("no está en la carta");

        clasica.setActivo(false);
        when(productos.findAllById(any())).thenReturn(List.of(clasica));
        assertThatThrownBy(() -> servicio.crear(ANTONIO, pedido(new Linea(5L, 2)))).hasMessageContaining("no está en la carta");
    }

    @Test
    void direccionDeOtraLocalidad() {
        Localidad estepa = new Localidad("Estepa", "Sevilla");
        ReflectionTestUtils.setField(estepa, "id", 2L);
        when(direcciones.findByIdAndClienteId(3L, ANTONIO_ID))
                .thenReturn(Optional.of(new Direccion(antonio, estepa, "Trabajo", "C/ X", "41560")));

        assertThatThrownBy(() -> servicio.crear(ANTONIO, pedido(new Linea(5L, 2)))).hasMessageContaining("no reparte en Estepa");
    }

    @Test
    void direccionAjenaYSinTelefono() {
        when(direcciones.findByIdAndClienteId(3L, ANTONIO_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servicio.crear(ANTONIO, pedido(new Linea(5L, 2)))).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void clienteSinTelefono() {
        antonio.setTelefono(null);
        assertThatThrownBy(() -> servicio.crear(ANTONIO, pedido(new Linea(5L, 2)))).hasMessageContaining("teléfono");
    }

    @Test
    void negocioSuspendidoNoExiste() {
        when(negocios.findByIdAndActivoTrue(7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servicio.crear(ANTONIO, pedido(new Linea(5L, 2)))).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void masDeCincuentaUnidadesSumandoLineas() {
        assertThatThrownBy(() -> servicio.crear(ANTONIO, pedido(new Linea(5L, 30), new Linea(5L, 30))))
                .hasMessageContaining("50 unidades");
    }

    @Test
    void pedidoDeOtroClienteNoExiste() {
        assertThatThrownBy(() -> servicio.obtener(ANTONIO, 99L)).isInstanceOf(RecursoNoEncontradoException.class);
    }
}
