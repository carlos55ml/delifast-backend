package dev.carlosmoran.delifast.controller;

import static dev.carlosmoran.delifast.support.SeguridadTest.admin;
import static dev.carlosmoran.delifast.support.SeguridadTest.gestor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.carlosmoran.delifast.dto.LocalidadResponse;
import dev.carlosmoran.delifast.dto.admin.AltaNegocioRequest;
import dev.carlosmoran.delifast.dto.admin.DatosNegocioRequest;
import dev.carlosmoran.delifast.dto.admin.NegocioAdminResponse;
import dev.carlosmoran.delifast.exception.ConflictoException;
import dev.carlosmoran.delifast.exception.RecursoNoEncontradoException;
import dev.carlosmoran.delifast.service.NegocioAdminService;
import dev.carlosmoran.delifast.support.SeguridadTest.ConSeguridad;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminNegocioController.class)
@ConSeguridad
class AdminNegocioControllerTest {

    private static final String ALTA = """
            {"nombre":"Heladería La Sirena","localidadId":1,"direccion":"C/ San Pedro 5",
             "telefono":"955 821 456","dueno":{"nombre":"Lola Sánchez","email":"lola@lasirena.es"}}""";

    private static final NegocioAdminResponse SIRENA = new NegocioAdminResponse(7L, "Heladería La Sirena",
            new LocalidadResponse(1L, "Osuna", "Sevilla"), "C/ San Pedro 5", "955 821 456",
            new NegocioAdminResponse.Dueno(UUID.fromString("00000000-0000-0000-0000-000000000007"), "Lola Sánchez", "lola@lasirena.es"),
            3, true, true, OffsetDateTime.parse("2026-11-06T13:10:00Z"));

    @Autowired
    MockMvc mvc;

    @MockitoBean
    NegocioAdminService servicio;

    @Test
    void listaConFiltroDeActivo() throws Exception {
        when(servicio.listar(false)).thenReturn(List.of(SIRENA));

        mvc.perform(get("/admin/negocios").param("activo", "false").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].localidad.nombre").value("Osuna"))
                .andExpect(jsonPath("$[0].dueno.email").value("lola@lasirena.es"))
                .andExpect(jsonPath("$[0].pedidosHoy").value(3))
                .andExpect(jsonPath("$[0].fechaAlta").value("2026-11-06T13:10:00Z"));
    }

    @Test
    void negocioSinDuenoDevuelveDuenoNull() throws Exception {
        NegocioAdminResponse sinDueno = new NegocioAdminResponse(8L, "Bar", SIRENA.localidad(), "C/ A", "955000000",
                null, 0, false, true, SIRENA.fechaAlta());
        when(servicio.listar(null)).thenReturn(List.of(sinDueno));

        mvc.perform(get("/admin/negocios").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dueno").isEmpty());
    }

    @Test
    void soloElAdministrador() throws Exception {
        mvc.perform(get("/admin/negocios").with(gestor(7))).andExpect(status().isForbidden());
        mvc.perform(get("/admin/negocios")).andExpect(status().isUnauthorized());
        verifyNoInteractions(servicio);
    }

    @Test
    void altaDevuelve201ConLocation() throws Exception {
        when(servicio.alta(any(AltaNegocioRequest.class))).thenReturn(SIRENA);

        mvc.perform(post("/admin/negocios").with(admin()).contentType(MediaType.APPLICATION_JSON).content(ALTA))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/admin/negocios/7"))
                .andExpect(jsonPath("$.nombre").value("Heladería La Sirena"));
    }

    @Test
    void altaConEmailRepetidoDevuelve409() throws Exception {
        when(servicio.alta(any(AltaNegocioRequest.class))).thenThrow(new ConflictoException("Ya existe una cuenta con ese email"));

        mvc.perform(post("/admin/negocios").with(admin()).contentType(MediaType.APPLICATION_JSON).content(ALTA))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Ya existe una cuenta con ese email"));
    }

    @Test
    void altaValidaLosCamposConFormatoCampoMensaje() throws Exception {
        String mal = """
                {"nombre":"","localidadId":1,"direccion":"C/ San Pedro 5","telefono":"123",
                 "dueno":{"nombre":"Lola","email":"no-es-un-email"}}""";

        mvc.perform(post("/admin/negocios").with(admin()).contentType(MediaType.APPLICATION_JSON).content(mal))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.length()").value(3))
                .andExpect(jsonPath("$.errores[?(@ =~ /nombre: .*/)]").exists())
                .andExpect(jsonPath("$.errores[?(@ =~ /telefono: .*/)]").exists())
                .andExpect(jsonPath("$.errores[?(@ =~ /dueno.email: .*/)]").exists());
        verifyNoInteractions(servicio);
    }

    @Test
    void editarNegocioInexistenteDevuelve404() throws Exception {
        when(servicio.editar(eq(99L), any(DatosNegocioRequest.class)))
                .thenThrow(new RecursoNoEncontradoException("No existe el negocio 99"));

        mvc.perform(put("/admin/negocios/99").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Bar","localidadId":1,"direccion":"C/ A","telefono":"955000000"}"""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void suspender() throws Exception {
        when(servicio.cambiarActivo(7L, false)).thenReturn(SIRENA);

        mvc.perform(patch("/admin/negocios/7/activo").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\":false}"))
                .andExpect(status().isOk());
    }

    @Test
    void cambiarActivoSinValorDevuelve400() throws Exception {
        mvc.perform(patch("/admin/negocios/7/activo").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0]").value(org.hamcrest.Matchers.startsWith("activo: ")));
    }
}
