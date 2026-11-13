package dev.carlosmoran.delifast.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import dev.carlosmoran.delifast.domain.Categoria;
import dev.carlosmoran.delifast.domain.Localidad;
import dev.carlosmoran.delifast.domain.Negocio;
import dev.carlosmoran.delifast.domain.Producto;
import dev.carlosmoran.delifast.domain.Rol;
import dev.carlosmoran.delifast.dto.negocio.CategoriaCartaResponse;
import dev.carlosmoran.delifast.dto.negocio.DatosProductoRequest;
import dev.carlosmoran.delifast.dto.negocio.ProductoCartaResponse;
import dev.carlosmoran.delifast.exception.ConflictoException;
import dev.carlosmoran.delifast.exception.RecursoNoEncontradoException;
import dev.carlosmoran.delifast.exception.ValidacionException;
import dev.carlosmoran.delifast.repository.CategoriaRepository;
import dev.carlosmoran.delifast.repository.NegocioRepository;
import dev.carlosmoran.delifast.repository.ProductoRepository;
import dev.carlosmoran.delifast.security.UsuarioAutenticado;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

class CartaServiceTest {

    private static final UsuarioAutenticado GESTOR = new UsuarioAutenticado(UUID.randomUUID(), Rol.NEGOCIO, 7L, true);

    private final NegocioRepository negocios = mock(NegocioRepository.class);
    private final CategoriaRepository categorias = mock(CategoriaRepository.class);
    private final ProductoRepository productos = mock(ProductoRepository.class);
    private final Imagenes imagenes = mock(Imagenes.class);
    private final CartaService servicio = new CartaService(new NegocioActual(negocios), categorias, productos, imagenes);

    private Negocio sirena;
    private Categoria hamburguesas;
    private Producto clasica;

    @BeforeEach
    void preparar() {
        sirena = new Negocio(new Localidad("Osuna", "Sevilla"), "La Sirena", "955000000", "C/ A");
        ReflectionTestUtils.setField(sirena, "id", 7L);
        when(negocios.findById(7L)).thenReturn(Optional.of(sirena));
        hamburguesas = new Categoria(sirena, "Hamburguesas", 0);
        ReflectionTestUtils.setField(hamburguesas, "id", 2L);
        when(categorias.findByIdAndNegocioId(2L, 7L)).thenReturn(Optional.of(hamburguesas));
        clasica = new Producto(hamburguesas, "Clásica", new BigDecimal("6.50"));
        ReflectionTestUtils.setField(clasica, "id", 5L);
        clasica.setFotoUrl("https://vieja");
        when(productos.findByIdAndCategoriaNegocioIdAndActivoTrue(5L, 7L)).thenReturn(Optional.of(clasica));
    }

    @Test
    void cartaSinBorradosYPorNombre() {
        Producto borrado = new Producto(hamburguesas, "Antigua", BigDecimal.ONE);
        borrado.setActivo(false);
        Producto agotada = new Producto(hamburguesas, "BBQ", BigDecimal.TEN);
        agotada.marcarAgotado();
        hamburguesas.getProductos().addAll(List.of(clasica, borrado, agotada));
        when(categorias.findByNegocioIdOrderByOrdenAscIdAsc(7L)).thenReturn(List.of(hamburguesas));

        List<CategoriaCartaResponse> carta = servicio.carta(GESTOR);

        assertThat(carta.getFirst().productos()).extracting(ProductoCartaResponse::nombre).containsExactly("BBQ", "Clásica");
    }

    @Test
    void nuevaCategoriaAlFinal() {
        when(categorias.ultimoOrden(7L)).thenReturn(3);
        when(categorias.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(servicio.crearCategoria(GESTOR, " Bebidas ").orden()).isEqualTo(4);
    }

    @Test
    void categoriaRepetidaEsConflicto() {
        when(categorias.existsByNegocioIdAndNombreIgnoreCase(7L, "hamburguesas")).thenReturn(true);

        assertThatThrownBy(() -> servicio.crearCategoria(GESTOR, "hamburguesas")).isInstanceOf(ConflictoException.class);
    }

    @Test
    void noSeBorraUnaCategoriaConProductos() {
        when(productos.existsByCategoriaIdAndActivoTrue(2L)).thenReturn(true);

        assertThatThrownBy(() -> servicio.borrarCategoria(GESTOR, 2L)).isInstanceOf(ConflictoException.class);
        verify(categorias, never()).delete(any());
    }

    @Test
    void categoriaDeOtroNegocioNoExiste() {
        assertThatThrownBy(() -> servicio.borrarCategoria(GESTOR, 99L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void productoEnCategoriaDeOtroNegocioEsError400() {
        DatosProductoRequest datos = new DatosProductoRequest(99L, "X", null, BigDecimal.ONE, true, null);

        assertThatThrownBy(() -> servicio.crearProducto(GESTOR, datos, null)).isInstanceOf(ValidacionException.class);
        verifyNoInteractions(imagenes);
    }

    @Test
    void crearProductoAgotadoConFoto() {
        MockMultipartFile foto = new MockMultipartFile("foto", new byte[] {1});
        when(imagenes.subir(foto, "foto", Imagenes.BUCKET_PRODUCTOS, 7L)).thenReturn("https://foto");
        when(productos.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProductoCartaResponse r = servicio.crearProducto(GESTOR,
                new DatosProductoRequest(2L, " BBQ ", " ", new BigDecimal("7.00"), false, null), foto);

        assertThat(r.nombre()).isEqualTo("BBQ");
        assertThat(r.descripcion()).isNull();
        assertThat(r.disponible()).isFalse();
        assertThat(r.fotoUrl()).isEqualTo("https://foto");
    }

    @Test
    void editarSinFotoConservaLaActual() {
        servicio.editarProducto(GESTOR, 5L, new DatosProductoRequest(2L, "Clásica", null, BigDecimal.TEN, true, null), null);

        assertThat(clasica.getFotoUrl()).isEqualTo("https://vieja");
        verifyNoInteractions(imagenes);
    }

    @Test
    void editarConQuitarFotoLaBorra() {
        servicio.editarProducto(GESTOR, 5L, new DatosProductoRequest(2L, "Clásica", null, BigDecimal.TEN, true, true), null);

        assertThat(clasica.getFotoUrl()).isNull();
        verify(imagenes).borrarAlConfirmar("https://vieja");
    }

    @Test
    void editarConFotoNuevaSustituyeLaAnterior() {
        MockMultipartFile foto = new MockMultipartFile("foto", new byte[] {1});
        when(imagenes.subir(foto, "foto", Imagenes.BUCKET_PRODUCTOS, 7L)).thenReturn("https://nueva");

        servicio.editarProducto(GESTOR, 5L, new DatosProductoRequest(2L, "Clásica", null, BigDecimal.TEN, true, null), foto);

        assertThat(clasica.getFotoUrl()).isEqualTo("https://nueva");
        verify(imagenes).borrarAlConfirmar("https://vieja");
    }

    @Test
    void borrarEsLogicoYQuitaLaFoto() {
        servicio.borrarProducto(GESTOR, 5L);

        assertThat(clasica.isActivo()).isFalse();
        assertThat(clasica.getFotoUrl()).isNull();
        verify(imagenes).borrarAlConfirmar("https://vieja");
        verify(productos, never()).delete(any());
    }

    @Test
    void agotarYReponer() {
        assertThat(servicio.cambiarDisponible(GESTOR, 5L, false).disponible()).isFalse();
        assertThat(servicio.cambiarDisponible(GESTOR, 5L, true).disponible()).isTrue();
    }
}
