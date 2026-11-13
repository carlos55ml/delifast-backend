package dev.carlosmoran.delifast.controller;

import static dev.carlosmoran.delifast.support.SeguridadTest.GESTOR_ID;
import static dev.carlosmoran.delifast.support.SeguridadTest.cliente;
import static dev.carlosmoran.delifast.support.SeguridadTest.gestor;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.carlosmoran.delifast.domain.EstadoIncidencia;
import dev.carlosmoran.delifast.domain.MetodoPago;
import dev.carlosmoran.delifast.domain.OrigenIncidencia;
import dev.carlosmoran.delifast.domain.Rol;
import dev.carlosmoran.delifast.domain.TipoIncidencia;
import dev.carlosmoran.delifast.dto.negocio.EstadisticasNegocioResponse;
import dev.carlosmoran.delifast.dto.negocio.IncidenciaNegocioResponse;
import dev.carlosmoran.delifast.dto.negocio.Periodo;
import dev.carlosmoran.delifast.exception.ConflictoException;
import dev.carlosmoran.delifast.security.UsuarioAutenticado;
import dev.carlosmoran.delifast.service.EstadisticasService;
import dev.carlosmoran.delifast.service.IncidenciaNegocioService;
import dev.carlosmoran.delifast.support.SeguridadTest.ConSeguridad;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(IncidenciaNegocioController.class)
@ConSeguridad
class IncidenciaNegocioControllerTest {

    private static final UsuarioAutenticado GESTOR = new UsuarioAutenticado(GESTOR_ID, Rol.NEGOCIO, 7L, true);
    private static final IncidenciaNegocioResponse RETRASO = new IncidenciaNegocioResponse(4L, TipoIncidencia.RETRASO,
            EstadoIncidencia.RESPONDIDA, OrigenIncidencia.CLIENTE, "Lleva una hora", "Ya sale",
            OffsetDateTime.parse("2026-11-13T12:00:00Z"), null, new IncidenciaNegocioResponse.Autor("Juan Torres"),
            new IncidenciaNegocioResponse.PedidoRef(36L, "DF-001036", new BigDecimal("18.50"), MetodoPago.TARJETA,
                    new IncidenciaNegocioResponse.Cliente("Juan Torres", "612345678")));

    @Autowired
    MockMvc mvc;

    @MockitoBean
    IncidenciaNegocioService incidencias;

    @MockitoBean
    EstadisticasService estadisticas;

    @Test
    void listaConEstados() throws Exception {
        when(incidencias.listar(GESTOR, List.of(EstadoIncidencia.ABIERTA, EstadoIncidencia.RESPONDIDA))).thenReturn(List.of(RETRASO));

        mvc.perform(get("/negocio/incidencias?estado=ABIERTA&estado=RESPONDIDA").with(gestor(7)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pedido.cliente.telefono").value("612345678"))
                .andExpect(jsonPath("$[0].autor.nombre").value("Juan Torres"));
    }

    @Test
    void responderYResolverSoloElNegocio() throws Exception {
        mvc.perform(post("/incidencias/4/responder").with(cliente()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"respuesta\":\"x\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/incidencias/4/resolver").with(cliente())).andExpect(status().isForbidden());
        verifyNoInteractions(incidencias);
    }

    @Test
    void responderExigeRespuesta() throws Exception {
        mvc.perform(post("/incidencias/4/responder").with(gestor(7)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"respuesta\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resolverSinCuerpo() throws Exception {
        when(incidencias.resolver(GESTOR, 4L, null)).thenReturn(RETRASO);

        mvc.perform(post("/incidencias/4/resolver").with(gestor(7))).andExpect(status().isOk());
        verify(incidencias).resolver(GESTOR, 4L, null);
    }

    @Test
    void resolverConRespuestaVaciaDevuelve400() throws Exception {
        mvc.perform(post("/incidencias/4/resolver").with(gestor(7)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"respuesta\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resolverUnaResueltaDevuelve409() throws Exception {
        when(incidencias.resolver(GESTOR, 4L, "Ok")).thenThrow(new ConflictoException("La incidencia ya está resuelta"));

        mvc.perform(post("/incidencias/4/resolver").with(gestor(7)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"respuesta\":\"Ok\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void estadisticas() throws Exception {
        when(estadisticas.calcular(GESTOR, Periodo.SEMANA, LocalDate.of(2026, 11, 13))).thenReturn(
                new EstadisticasNegocioResponse(Periodo.SEMANA, LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 15),
                        10, 8, new BigDecimal("150.00"), new BigDecimal("120.00"), 31, null,
                        new EstadisticasNegocioResponse.Incidencias(2, 1, 1),
                        List.of(new EstadisticasNegocioResponse.Tramo("Lun", 3)),
                        List.of(new EstadisticasNegocioResponse.ProductoVendido("Clásica", 12))));

        mvc.perform(get("/negocio/estadisticas?periodo=SEMANA&fecha=2026-11-13").with(gestor(7)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desde").value("2026-11-09"))
                .andExpect(jsonPath("$.facturacion").value(150.00))
                .andExpect(jsonPath("$.tiempoMedioEntregaAnteriorMin").isEmpty())
                .andExpect(jsonPath("$.masVendidos[0].unidades").value(12));
    }

    @Test
    void estadisticasSinPeriodoOConFechaMalDevuelve400() throws Exception {
        mvc.perform(get("/negocio/estadisticas").with(gestor(7)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0]").value("periodo: es obligatorio"));
        mvc.perform(get("/negocio/estadisticas?periodo=ANIO").with(gestor(7))).andExpect(status().isBadRequest());
        mvc.perform(get("/negocio/estadisticas?periodo=DIA&fecha=13-11-2026").with(gestor(7)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0]").value("fecha: valor no válido"));
    }
}
