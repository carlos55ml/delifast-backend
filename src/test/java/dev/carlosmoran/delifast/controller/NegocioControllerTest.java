package dev.carlosmoran.delifast.controller;

import static dev.carlosmoran.delifast.support.SeguridadTest.GESTOR_ID;
import static dev.carlosmoran.delifast.support.SeguridadTest.admin;
import static dev.carlosmoran.delifast.support.SeguridadTest.cliente;
import static dev.carlosmoran.delifast.support.SeguridadTest.gestor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.carlosmoran.delifast.domain.EstadoPedido;
import dev.carlosmoran.delifast.domain.MetodoPago;
import dev.carlosmoran.delifast.dto.LocalidadResponse;
import dev.carlosmoran.delifast.dto.negocio.DatosMiNegocioRequest;
import dev.carlosmoran.delifast.dto.negocio.FranjaHorarioDto;
import dev.carlosmoran.delifast.dto.negocio.MiNegocioResponse;
import dev.carlosmoran.delifast.dto.negocio.PedidoNegocioResponse;
import dev.carlosmoran.delifast.exception.ConflictoException;
import dev.carlosmoran.delifast.exception.RecursoNoEncontradoException;
import dev.carlosmoran.delifast.exception.ReglaNegocioException;
import dev.carlosmoran.delifast.security.UsuarioAutenticado;
import dev.carlosmoran.delifast.service.MiNegocioService;
import dev.carlosmoran.delifast.service.PedidoNegocioService;
import dev.carlosmoran.delifast.support.SeguridadTest.ConSeguridad;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NegocioController.class)
@ConSeguridad
class NegocioControllerTest {

    private static final UsuarioAutenticado GESTOR = new UsuarioAutenticado(GESTOR_ID,
            dev.carlosmoran.delifast.domain.Rol.NEGOCIO, 7L, true);
    private static final UUID MARTA = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    private static final PedidoNegocioResponse PEDIDO = new PedidoNegocioResponse(43L, "DF-001043",
            EstadoPedido.EN_PREPARACION, OffsetDateTime.parse("2026-11-13T12:00:00Z"),
            new PedidoNegocioResponse.Cliente("Antonio Pérez"), "Av. de la Constitución 8 · Osuna", "612345678",
            MetodoPago.EFECTIVO, null,
            List.of(new PedidoNegocioResponse.Linea("Menú DeliFast", new BigDecimal("8.50"), 2, new BigDecimal("17.00"))),
            new BigDecimal("17.00"), new BigDecimal("1.50"), new BigDecimal("18.50"), 25, null, null,
            List.of(new PedidoNegocioResponse.Cambio(null, EstadoPedido.RECIBIDO, OffsetDateTime.parse("2026-11-13T12:00:00Z"), "Antonio Pérez"),
                    new PedidoNegocioResponse.Cambio(EstadoPedido.RECIBIDO, EstadoPedido.EN_PREPARACION, OffsetDateTime.parse("2026-11-13T12:02:00Z"), "Lola")));

    private static MiNegocioResponse miNegocio(boolean abierto, long pedidosHoy) {
        return new MiNegocioResponse(7L, "La Sirena", null, new LocalidadResponse(1L, "Osuna", "Sevilla"),
                "C/ San Pedro 5", "955821456", "https://x.supabase.co/storage/v1/object/public/logos/7/a.png",
                new BigDecimal("1.50"), new BigDecimal("10.00"),
                List.of(new FranjaHorarioDto(1, "20:00", "00:30")), abierto, abierto, pedidosHoy);
    }

    @Autowired
    MockMvc mvc;

    @MockitoBean
    MiNegocioService miNegocio;

    @MockitoBean
    PedidoNegocioService pedidos;

    @Test
    void cabeceraDelNegocioDelToken() throws Exception {
        when(miNegocio.obtener(GESTOR)).thenReturn(miNegocio(true, 12));

        mvc.perform(get("/negocio").with(gestor(7)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.abiertoManual").value(true))
                .andExpect(jsonPath("$.pedidosHoy").value(12))
                .andExpect(jsonPath("$.gastosEnvio").value(1.5))
                .andExpect(jsonPath("$.horarios[0].cierre").value("00:30"))
                .andExpect(jsonPath("$.descripcion").isEmpty());
    }

    @Test
    void editarConLogoEnMultipart() throws Exception {
        MockMultipartFile datos = new MockMultipartFile("datos", "", MediaType.APPLICATION_JSON_VALUE, """
                {"nombre":"La Sirena","telefono":"955821456","direccion":"C/ San Pedro 5",
                 "gastosEnvio":1.5,"pedidoMinimo":10}""".getBytes());
        MockMultipartFile logo = new MockMultipartFile("logo", "logo.png", "image/png", new byte[] {1, 2});
        when(miNegocio.editar(eq(GESTOR), any(), any())).thenReturn(miNegocio(true, 0));

        mvc.perform(multipart(HttpMethod.PUT, "/negocio").file(datos).file(logo).with(gestor(7)))
                .andExpect(status().isOk());
        verify(miNegocio).editar(eq(GESTOR), eq(new DatosMiNegocioRequest("La Sirena", null, "955821456",
                "C/ San Pedro 5", new BigDecimal("1.5"), new BigDecimal("10"))), eq(logo));
    }

    @Test
    void editarValidaLaParteDatos() throws Exception {
        MockMultipartFile datos = new MockMultipartFile("datos", "", MediaType.APPLICATION_JSON_VALUE, """
                {"nombre":"La Sirena","telefono":"955821456","direccion":"C/ San Pedro 5",
                 "gastosEnvio":60,"pedidoMinimo":10}""".getBytes());

        mvc.perform(multipart(HttpMethod.PUT, "/negocio").file(datos).with(gestor(7)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0]").value(org.hamcrest.Matchers.startsWith("gastosEnvio: ")));
        verifyNoInteractions(miNegocio);
    }

    @Test
    void editarSinParteDatosDevuelve400() throws Exception {
        mvc.perform(multipart(HttpMethod.PUT, "/negocio").with(gestor(7)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0]").value("datos: es obligatorio"));
    }

    @Test
    void horariosValidaCadaFranja() throws Exception {
        mvc.perform(put("/negocio/horarios").with(gestor(7)).contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"dia\":1,\"apertura\":\"12:00\",\"cierre\":\"16:00\"},"
                                + "{\"dia\":8,\"apertura\":\"25:00\",\"cierre\":\"16:00\"}]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.length()").value(2))
                .andExpect(jsonPath("$.errores[?(@ =~ /horarios\\[1\\]\\.dia: .*/)]").exists())
                .andExpect(jsonPath("$.errores[?(@ =~ /horarios\\[1\\]\\.apertura: .*/)]").exists());
        verifyNoInteractions(miNegocio);
    }

    @Test
    void horarios() throws Exception {
        List<FranjaHorarioDto> franjas = List.of(new FranjaHorarioDto(1, "20:00", "00:30"));
        when(miNegocio.cambiarHorarios(GESTOR, franjas)).thenReturn(miNegocio(true, 0));

        mvc.perform(put("/negocio/horarios").with(gestor(7)).contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"dia\":1,\"apertura\":\"20:00\",\"cierre\":\"00:30\"}]"))
                .andExpect(status().isOk());
    }

    @Test
    void soloRolNegocio() throws Exception {
        mvc.perform(get("/negocio").with(admin())).andExpect(status().isForbidden());
        mvc.perform(get("/negocio/pedidos").with(cliente())).andExpect(status().isForbidden());
        mvc.perform(post("/pedidos/43/listo").with(cliente())).andExpect(status().isForbidden());
        mvc.perform(post("/pedidos/43/listo")).andExpect(status().isUnauthorized());
        verifyNoInteractions(miNegocio, pedidos);
    }

    @Test
    void interruptorAbierto() throws Exception {
        when(miNegocio.cambiarAbierto(GESTOR, false)).thenReturn(miNegocio(false, 0));

        mvc.perform(patch("/negocio/abierto").with(gestor(7)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"abierto\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.abierto").value(false));
    }

    @Test
    void tableroConFiltroDeEstados() throws Exception {
        when(pedidos.listar(GESTOR, List.of(EstadoPedido.RECIBIDO, EstadoPedido.LISTO))).thenReturn(List.of(PEDIDO));

        mvc.perform(get("/negocio/pedidos?estado=RECIBIDO&estado=LISTO").with(gestor(7)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("DF-001043"))
                .andExpect(jsonPath("$[0].cliente.nombre").value("Antonio Pérez"))
                .andExpect(jsonPath("$[0].lineas[0].importe").value(17.00))
                .andExpect(jsonPath("$[0].reparto").isEmpty())
                .andExpect(jsonPath("$[0].historial[0].estadoAnterior").isEmpty())
                .andExpect(jsonPath("$[0].historial[1].autor").value("Lola"));
    }

    @Test
    void pedidoDeOtroNegocioDevuelve404() throws Exception {
        when(pedidos.obtener(GESTOR, 99L)).thenThrow(new RecursoNoEncontradoException("No existe el pedido 99"));

        mvc.perform(get("/negocio/pedidos/99").with(gestor(7))).andExpect(status().isNotFound());
    }

    @Test
    void aceptarValidaElTiempo() throws Exception {
        mvc.perform(post("/pedidos/43/aceptar").with(gestor(7)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tiempoEstimadoMin\":3}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0]").value(org.hamcrest.Matchers.startsWith("tiempoEstimadoMin: ")));
        verifyNoInteractions(pedidos);
    }

    @Test
    void aceptar() throws Exception {
        when(pedidos.aceptar(GESTOR, 43L, 25)).thenReturn(PEDIDO);

        mvc.perform(post("/pedidos/43/aceptar").with(gestor(7)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tiempoEstimadoMin\":25}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_PREPARACION"))
                .andExpect(jsonPath("$.tiempoEstimadoMin").value(25));
    }

    @Test
    void siOtroSeAdelantoDevuelve409() throws Exception {
        when(pedidos.rechazar(eq(GESTOR), eq(43L), any()))
                .thenThrow(new ConflictoException("El pedido DF-001043 ya no está RECIBIDO: ahora está EN_PREPARACION"));

        mvc.perform(post("/pedidos/43/rechazar").with(gestor(7)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Sin pan\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void rechazarSinMotivoDevuelve400() throws Exception {
        mvc.perform(post("/pedidos/43/rechazar").with(gestor(7)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listo() throws Exception {
        when(pedidos.marcarListo(GESTOR, 43L)).thenReturn(PEDIDO);

        mvc.perform(post("/pedidos/43/listo").with(gestor(7))).andExpect(status().isOk());
        verify(pedidos).marcarListo(GESTOR, 43L);
    }

    @Test
    void asignarRepartidorNoAsignableDevuelve422() throws Exception {
        when(pedidos.asignar(GESTOR, 43L, MARTA)).thenThrow(new ReglaNegocioException("El repartidor no está disponible"));

        mvc.perform(post("/pedidos/43/asignar").with(gestor(7)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"repartidorId\":\"" + MARTA + "\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("El repartidor no está disponible"));
    }

    @Test
    void asignarSinRepartidorDevuelve400() throws Exception {
        mvc.perform(post("/pedidos/43/asignar").with(gestor(7)).contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
