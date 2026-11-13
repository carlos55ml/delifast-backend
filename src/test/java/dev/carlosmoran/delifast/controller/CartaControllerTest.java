package dev.carlosmoran.delifast.controller;

import static dev.carlosmoran.delifast.support.SeguridadTest.GESTOR_ID;
import static dev.carlosmoran.delifast.support.SeguridadTest.cliente;
import static dev.carlosmoran.delifast.support.SeguridadTest.gestor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.carlosmoran.delifast.domain.Rol;
import dev.carlosmoran.delifast.dto.negocio.CategoriaCartaResponse;
import dev.carlosmoran.delifast.dto.negocio.DatosProductoRequest;
import dev.carlosmoran.delifast.dto.negocio.ProductoCartaResponse;
import dev.carlosmoran.delifast.exception.ConflictoException;
import dev.carlosmoran.delifast.exception.ValidacionException;
import dev.carlosmoran.delifast.security.UsuarioAutenticado;
import dev.carlosmoran.delifast.service.CartaService;
import dev.carlosmoran.delifast.support.SeguridadTest.ConSeguridad;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CartaController.class)
@ConSeguridad
class CartaControllerTest {

    private static final UsuarioAutenticado GESTOR = new UsuarioAutenticado(GESTOR_ID, Rol.NEGOCIO, 7L, true);
    private static final ProductoCartaResponse CLASICA = new ProductoCartaResponse(5L, 2L, "Hamburguesa clásica",
            null, new BigDecimal("6.50"), null, true);
    private static final String DATOS = """
            {"categoriaId":2,"nombre":"Hamburguesa clásica","precio":6.5,"disponible":true}""";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    CartaService carta;

    @Test
    void cartaCompleta() throws Exception {
        when(carta.carta(GESTOR)).thenReturn(List.of(new CategoriaCartaResponse(2L, "Hamburguesas", 0, List.of(CLASICA))));

        mvc.perform(get("/negocio/carta").with(gestor(7)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productos[0].precio").value(6.5))
                .andExpect(jsonPath("$[0].productos[0].fotoUrl").isEmpty());
    }

    @Test
    void soloElNegocio() throws Exception {
        mvc.perform(get("/negocio/carta").with(cliente())).andExpect(status().isForbidden());
        verifyNoInteractions(carta);
    }

    @Test
    void crearCategoriaDevuelve201() throws Exception {
        when(carta.crearCategoria(GESTOR, "Bebidas")).thenReturn(new CategoriaCartaResponse(3L, "Bebidas", 1, List.of()));

        mvc.perform(post("/negocio/categorias").with(gestor(7)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Bebidas\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/negocio/categorias/3"));
    }

    @Test
    void categoriaRepetidaDevuelve409() throws Exception {
        when(carta.crearCategoria(GESTOR, "Bebidas")).thenThrow(new ConflictoException("Ya hay una categoría \"Bebidas\" en la carta"));

        mvc.perform(post("/negocio/categorias").with(gestor(7)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Bebidas\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void borrarCategoriaConProductosDevuelve409() throws Exception {
        doThrow(new ConflictoException("La categoría aún tiene productos")).when(carta).borrarCategoria(GESTOR, 2L);

        mvc.perform(delete("/negocio/categorias/2").with(gestor(7))).andExpect(status().isConflict());
    }

    @Test
    void borrarCategoriaVaciaDevuelve204() throws Exception {
        mvc.perform(delete("/negocio/categorias/3").with(gestor(7))).andExpect(status().isNoContent());
        verify(carta).borrarCategoria(GESTOR, 3L);
    }

    @Test
    void crearProductoConFoto() throws Exception {
        MockMultipartFile foto = new MockMultipartFile("foto", "f.jpg", "image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
        when(carta.crearProducto(eq(GESTOR), any(), any())).thenReturn(CLASICA);

        mvc.perform(multipart("/negocio/productos").file(datos(DATOS)).file(foto).with(gestor(7)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/negocio/productos/5"));
        verify(carta).crearProducto(eq(GESTOR), eq(new DatosProductoRequest(2L, "Hamburguesa clásica", null,
                new BigDecimal("6.5"), true, null)), eq(foto));
    }

    @Test
    void crearProductoSinFoto() throws Exception {
        when(carta.crearProducto(eq(GESTOR), any(), isNull())).thenReturn(CLASICA);

        mvc.perform(multipart("/negocio/productos").file(datos(DATOS)).with(gestor(7)))
                .andExpect(status().isCreated());
    }

    @Test
    void fotoNoValidaDevuelve400ConElCampo() throws Exception {
        MockMultipartFile foto = new MockMultipartFile("foto", "f.gif", "image/gif", new byte[] {1});
        when(carta.crearProducto(eq(GESTOR), any(), any()))
                .thenThrow(new ValidacionException("foto", "tiene que ser una imagen JPEG, PNG o WebP"));

        mvc.perform(multipart("/negocio/productos").file(datos(DATOS)).file(foto).with(gestor(7)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0]").value("foto: tiene que ser una imagen JPEG, PNG o WebP"));
    }

    @Test
    void productoValidaPrecio() throws Exception {
        mvc.perform(multipart("/negocio/productos")
                        .file(datos("{\"categoriaId\":2,\"nombre\":\"X\",\"precio\":1000,\"disponible\":true}"))
                        .with(gestor(7)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0]").value(org.hamcrest.Matchers.startsWith("precio: ")));
        verifyNoInteractions(carta);
    }

    @Test
    void editarProductoQuitandoFoto() throws Exception {
        when(carta.editarProducto(eq(GESTOR), eq(5L), any(), isNull())).thenReturn(CLASICA);

        mvc.perform(multipart(HttpMethod.PUT, "/negocio/productos/5")
                        .file(datos("{\"categoriaId\":2,\"nombre\":\"Hamburguesa clásica\",\"precio\":6.5,\"disponible\":true,\"quitarFoto\":true}"))
                        .with(gestor(7)))
                .andExpect(status().isOk());
        verify(carta).editarProducto(eq(GESTOR), eq(5L), eq(new DatosProductoRequest(2L, "Hamburguesa clásica", null,
                new BigDecimal("6.5"), true, true)), isNull());
    }

    @Test
    void borrarYAgotarProducto() throws Exception {
        mvc.perform(delete("/negocio/productos/5").with(gestor(7))).andExpect(status().isNoContent());
        verify(carta).borrarProducto(GESTOR, 5L);

        when(carta.cambiarDisponible(GESTOR, 5L, false)).thenReturn(CLASICA);
        mvc.perform(patch("/negocio/productos/5/disponible").with(gestor(7)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disponible\":false}"))
                .andExpect(status().isOk());
    }

    private static MockMultipartFile datos(String json) {
        return new MockMultipartFile("datos", "", MediaType.APPLICATION_JSON_VALUE, json.getBytes());
    }
}
