package dev.carlosmoran.delifast.security;

import static dev.carlosmoran.delifast.support.SeguridadTest.admin;
import static dev.carlosmoran.delifast.support.SeguridadTest.cliente;
import static dev.carlosmoran.delifast.support.SeguridadTest.gestor;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.carlosmoran.delifast.support.SeguridadTest.ConSeguridad;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Reglas de acceso por rol y formato de los 401/403, con un controlador de prueba. */
@WebMvcTest(controllers = SecurityConfigTest.Rutas.class)
@ConSeguridad
@Import(SecurityConfigTest.Rutas.class)
class SecurityConfigTest {

    @RestController
    static class Rutas {
        @GetMapping("/admin/prueba")
        String admin() { return "ok"; }

        @GetMapping("/negocio/prueba")
        String negocio() { return "ok"; }

        @GetMapping("/prueba")
        String autenticado() { return "ok"; }
    }

    @Autowired
    MockMvc mvc;

    @Test
    void sinTokenDevuelve401ConProblemDetail() throws Exception {
        mvc.perform(get("/admin/prueba"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.instance").value("/admin/prueba"));
    }

    @Test
    void adminEntraEnAdmin() throws Exception {
        mvc.perform(get("/admin/prueba").with(admin())).andExpect(status().isOk());
    }

    @Test
    void otroRolEnAdminDevuelve403ConProblemDetail() throws Exception {
        mvc.perform(get("/admin/prueba").with(gestor(1)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void zonaNegocioSoloParaGestores() throws Exception {
        mvc.perform(get("/negocio/prueba").with(gestor(1))).andExpect(status().isOk());
        mvc.perform(get("/negocio/prueba").with(admin())).andExpect(status().isForbidden());
    }

    @Test
    void elRestoSoloPideEstarAutenticado() throws Exception {
        mvc.perform(get("/prueba").with(cliente())).andExpect(status().isOk());
        mvc.perform(get("/prueba")).andExpect(status().isUnauthorized());
    }
}
