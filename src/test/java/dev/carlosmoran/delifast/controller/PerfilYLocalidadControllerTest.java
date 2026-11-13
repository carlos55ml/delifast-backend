package dev.carlosmoran.delifast.controller;

import static dev.carlosmoran.delifast.support.SeguridadTest.GESTOR_ID;
import static dev.carlosmoran.delifast.support.SeguridadTest.gestor;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.carlosmoran.delifast.domain.Rol;
import dev.carlosmoran.delifast.dto.LocalidadResponse;
import dev.carlosmoran.delifast.dto.PerfilResponse;
import dev.carlosmoran.delifast.service.LocalidadService;
import dev.carlosmoran.delifast.service.PerfilService;
import dev.carlosmoran.delifast.support.SeguridadTest.ConSeguridad;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({PerfilController.class, LocalidadController.class})
@ConSeguridad
class PerfilYLocalidadControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    PerfilService perfiles;

    @MockitoBean
    LocalidadService localidades;

    @Test
    void localidadesEsPublica() throws Exception {
        when(localidades.listar()).thenReturn(List.of(new LocalidadResponse(1L, "Osuna", "Sevilla")));

        mvc.perform(get("/localidades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Osuna"));
    }

    @Test
    void meDevuelveElPerfilDelToken() throws Exception {
        when(perfiles.obtener(GESTOR_ID)).thenReturn(
                new PerfilResponse(GESTOR_ID, "Lola", "lola@lasirena.es", null, Rol.NEGOCIO, 7L, true));

        mvc.perform(get("/me").with(gestor(7)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("NEGOCIO"))
                .andExpect(jsonPath("$.negocioId").value(7))
                .andExpect(jsonPath("$.esDueno").value(true))
                .andExpect(jsonPath("$.telefono").isEmpty());
    }

    @Test
    void meSinTokenDevuelve401() throws Exception {
        mvc.perform(get("/me")).andExpect(status().isUnauthorized());
    }
}
