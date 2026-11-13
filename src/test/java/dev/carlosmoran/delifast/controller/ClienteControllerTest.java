package dev.carlosmoran.delifast.controller;

import static dev.carlosmoran.delifast.support.SeguridadTest.cliente;
import static dev.carlosmoran.delifast.support.SeguridadTest.gestor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.carlosmoran.delifast.domain.EstadoPedido;
import dev.carlosmoran.delifast.domain.MetodoPago;
import dev.carlosmoran.delifast.dto.LocalidadResponse;
import dev.carlosmoran.delifast.dto.cliente.CartaClienteResponse;
import dev.carlosmoran.delifast.dto.cliente.NegocioClienteResponse;
import dev.carlosmoran.delifast.dto.cliente.PedidoClienteResponse;
import dev.carlosmoran.delifast.exception.ReglaNegocioException;
import dev.carlosmoran.delifast.service.CatalogoClienteService;
import dev.carlosmoran.delifast.service.DireccionService;
import dev.carlosmoran.delifast.service.PedidoClienteService;
import dev.carlosmoran.delifast.support.SeguridadTest.ConSeguridad;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ClienteController.class)
@ConSeguridad
class ClienteControllerTest {

    private static final NegocioClienteResponse SIRENA = new NegocioClienteResponse(7L, "La Sirena", null, "C/ A",
            "955000000", new LocalidadResponse(1L, "Osuna", "Sevilla"), null, new BigDecimal("1.50"),
            new BigDecimal("10.00"), true);
    private static final PedidoClienteResponse PEDIDO = new PedidoClienteResponse(48L, "DF-001048",
            new PedidoClienteResponse.NegocioRef(7L, "La Sirena"), EstadoPedido.RECIBIDO, MetodoPago.EFECTIVO, null,
            "C/ Mayor 1, 41640 Osuna", new BigDecimal("13.00"), new BigDecimal("1.50"), new BigDecimal("14.50"),
            null, null, OffsetDateTime.parse("2026-11-13T12:00:00Z"),
            List.of(new PedidoClienteResponse.Linea(5L, "Clásica", new BigDecimal("6.50"), 2)),
            List.of(new PedidoClienteResponse.Cambio(EstadoPedido.RECIBIDO, OffsetDateTime.parse("2026-11-13T12:00:00Z"))));
    private static final String NUEVO = """
            {"negocioId":7,"direccionId":3,"metodoPago":"EFECTIVO","lineas":[{"productoId":5,"cantidad":2}]}""";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    CatalogoClienteService catalogo;

    @MockitoBean
    DireccionService direcciones;

    @MockitoBean
    PedidoClienteService pedidos;

    @Test
    void negociosYCartaSonPublicos() throws Exception {
        when(catalogo.negocios(1L)).thenReturn(List.of(SIRENA));
        when(catalogo.carta(7L)).thenReturn(new CartaClienteResponse(SIRENA, List.of(
                new CartaClienteResponse.CategoriaCliente(2L, "Hamburguesas", List.of(
                        new CartaClienteResponse.ProductoCliente(5L, "Clásica", null, new BigDecimal("6.50"), null, false))))));

        mvc.perform(get("/negocios?localidadId=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].abierto").value(true))
                .andExpect(jsonPath("$[0].localidad.nombre").value("Osuna"));
        mvc.perform(get("/negocios/7/carta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.negocio.id").value(7))
                .andExpect(jsonPath("$.categorias[0].productos[0].disponible").value(false));
    }

    @Test
    void crearPedidoDevuelve201() throws Exception {
        when(pedidos.crear(any(), any())).thenReturn(PEDIDO);

        mvc.perform(post("/pedidos").with(cliente()).contentType(MediaType.APPLICATION_JSON).content(NUEVO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/pedidos/48"))
                .andExpect(jsonPath("$.lineas[0].productoId").value(5))
                .andExpect(jsonPath("$.historial[0].estado").value("RECIBIDO"));
    }

    @Test
    void reglaIncumplidaDevuelve422() throws Exception {
        when(pedidos.crear(any(), any())).thenThrow(new ReglaNegocioException("La Sirena está cerrado ahora mismo"));

        mvc.perform(post("/pedidos").with(cliente()).contentType(MediaType.APPLICATION_JSON).content(NUEVO))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("La Sirena está cerrado ahora mismo"));
    }

    @Test
    void pedidoSinLineasDevuelve400() throws Exception {
        mvc.perform(post("/pedidos").with(cliente()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"negocioId\":7,\"direccionId\":3,\"metodoPago\":\"EFECTIVO\",\"lineas\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0]").value(org.hamcrest.Matchers.startsWith("lineas: ")));
        verifyNoInteractions(pedidos);
    }

    @Test
    void cantidadFueraDeRangoDevuelve400() throws Exception {
        mvc.perform(post("/pedidos").with(cliente()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"negocioId\":7,\"direccionId\":3,\"metodoPago\":\"EFECTIVO\",\"lineas\":[{\"productoId\":5,\"cantidad\":0}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0]").value(org.hamcrest.Matchers.startsWith("lineas[0].cantidad: ")));
    }

    @Test
    void soloClientes() throws Exception {
        mvc.perform(post("/pedidos").with(gestor(7)).contentType(MediaType.APPLICATION_JSON).content(NUEVO))
                .andExpect(status().isForbidden());
        mvc.perform(get("/pedidos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/me/direcciones").with(gestor(7))).andExpect(status().isForbidden());
        verifyNoInteractions(pedidos, direcciones);
    }

    @Test
    void direccionValidaCodigoPostal() throws Exception {
        mvc.perform(post("/me/direcciones").with(cliente()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"alias\":\"Casa\",\"calle\":\"C/ Mayor 1\",\"codigoPostal\":\"4164\",\"localidadId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0]").value("codigoPostal: debe tener 5 dígitos"));
    }
}
