package dev.carlosmoran.delifast.controller;

import static dev.carlosmoran.delifast.support.SeguridadTest.REPARTIDOR_ID;
import static dev.carlosmoran.delifast.support.SeguridadTest.gestor;
import static dev.carlosmoran.delifast.support.SeguridadTest.repartidor;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.carlosmoran.delifast.domain.EstadoPedido;
import dev.carlosmoran.delifast.domain.MetodoPago;
import dev.carlosmoran.delifast.domain.Rol;
import dev.carlosmoran.delifast.dto.repartidor.RepartoRepartidorResponse;
import dev.carlosmoran.delifast.exception.ConflictoException;
import dev.carlosmoran.delifast.security.UsuarioAutenticado;
import dev.carlosmoran.delifast.service.RepartidorService;
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

@WebMvcTest(RepartidorController.class)
@ConSeguridad
class RepartidorControllerTest {

    private static final UsuarioAutenticado MARTA = new UsuarioAutenticado(REPARTIDOR_ID, Rol.REPARTIDOR, 7L, false);
    private static final RepartoRepartidorResponse REPARTO = new RepartoRepartidorResponse(30L, EstadoPedido.EN_REPARTO,
            OffsetDateTime.parse("2026-11-13T12:00:00Z"), null, null,
            new RepartoRepartidorResponse.PedidoRef(50L, "DF-001050", new RepartoRepartidorResponse.Cliente("Juan"),
                    "C/ Carrera 12, 41640 Osuna", null, null, "612345678", MetodoPago.TARJETA, null, List.of(),
                    new BigDecimal("17.00"), new BigDecimal("1.50"), new BigDecimal("18.50")));

    @Autowired
    MockMvc mvc;

    @MockitoBean
    RepartidorService servicio;

    @Test
    void misRepartos() throws Exception {
        when(servicio.repartos(MARTA, EstadoPedido.EN_REPARTO)).thenReturn(List.of(REPARTO));

        mvc.perform(get("/repartidor/repartos?estado=EN_REPARTO").with(repartidor(7)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pedido.telefonoContacto").value("612345678"))
                .andExpect(jsonPath("$[0].pedido.latitud").isEmpty())
                .andExpect(jsonPath("$[0].metodoCobro").isEmpty());
    }

    @Test
    void estadoNoPermitidoDevuelve400() throws Exception {
        mvc.perform(get("/repartidor/repartos?estado=LISTO").with(repartidor(7)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0]").value("estado: solo admite EN_REPARTO o ENTREGADO"));
    }

    @Test
    void soloRepartidores() throws Exception {
        mvc.perform(get("/repartidor/perfil").with(gestor(7))).andExpect(status().isForbidden());
        mvc.perform(post("/repartos/30/entregar").with(gestor(7)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"metodoCobro\":\"EFECTIVO\"}")).andExpect(status().isForbidden());
        verifyNoInteractions(servicio);
    }

    @Test
    void entregarYaEntregadoDevuelve409() throws Exception {
        when(servicio.entregar(MARTA, 30L, MetodoPago.EFECTIVO)).thenThrow(new ConflictoException("ya no está EN_REPARTO"));

        mvc.perform(post("/repartos/30/entregar").with(repartidor(7)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metodoCobro\":\"EFECTIVO\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void entregarSinMetodoDevuelve400() throws Exception {
        mvc.perform(post("/repartos/30/entregar").with(repartidor(7)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void incidenciaDevuelve201() throws Exception {
        mvc.perform(post("/repartos/30/incidencias").with(repartidor(7)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"CLIENTE_AUSENTE\"}"))
                .andExpect(status().isCreated());
    }
}
