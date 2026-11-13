package dev.carlosmoran.delifast.controller;

import static dev.carlosmoran.delifast.support.SeguridadTest.admin;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.carlosmoran.delifast.domain.EstadoIncidencia;
import dev.carlosmoran.delifast.domain.OrigenIncidencia;
import dev.carlosmoran.delifast.domain.TipoIncidencia;
import dev.carlosmoran.delifast.dto.admin.IncidenciaAdminResponse;
import dev.carlosmoran.delifast.dto.admin.ResumenAdminResponse;
import dev.carlosmoran.delifast.service.AdminConsultaService;
import dev.carlosmoran.delifast.support.SeguridadTest.ConSeguridad;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminConsultaController.class)
@ConSeguridad
class AdminConsultaControllerTest {

    private static final IncidenciaAdminResponse RETRASO = new IncidenciaAdminResponse(4L, TipoIncidencia.RETRASO,
            EstadoIncidencia.ABIERTA, OrigenIncidencia.CLIENTE, "Lleva una hora", null,
            OffsetDateTime.parse("2026-11-06T13:10:00Z"), null,
            new IncidenciaAdminResponse.PedidoRef(36L, "DF-001036"),
            new IncidenciaAdminResponse.NegocioRef(7L, "Heladería La Sirena"));

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AdminConsultaService servicio;

    @Test
    void incidenciasConVariosEstadosYNegocio() throws Exception {
        when(servicio.incidencias(List.of(EstadoIncidencia.ABIERTA, EstadoIncidencia.RESPONDIDA), 7L))
                .thenReturn(List.of(RETRASO));

        mvc.perform(get("/admin/incidencias?estado=ABIERTA&estado=RESPONDIDA&negocioId=7").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pedido.codigo").value("DF-001036"))
                .andExpect(jsonPath("$[0].negocio.nombre").value("Heladería La Sirena"))
                .andExpect(jsonPath("$[0].respuesta").isEmpty());
    }

    @Test
    void estadoDesconocidoDevuelve400() throws Exception {
        mvc.perform(get("/admin/incidencias?estado=PERDIDA").with(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0]").value("estado: valor no válido"));
    }

    @Test
    void resumen() throws Exception {
        when(servicio.resumen()).thenReturn(new ResumenAdminResponse(2, 3, 10, 2, 4, 1, 1,
                List.of(new ResumenAdminResponse.PedidosNegocioHoy(7L, "Heladería La Sirena", "Osuna", 6, true)),
                List.of(RETRASO)));

        mvc.perform(get("/admin/resumen").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.negociosActivos").value(2))
                .andExpect(jsonPath("$.pedidosHoyPorNegocio[0].localidad").value("Osuna"))
                .andExpect(jsonPath("$.incidenciasRecientes[0].id").value(4));
    }
}
