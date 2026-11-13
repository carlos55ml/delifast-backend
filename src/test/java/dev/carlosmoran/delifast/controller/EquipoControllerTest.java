package dev.carlosmoran.delifast.controller;

import static dev.carlosmoran.delifast.support.SeguridadTest.GESTOR_ID;
import static dev.carlosmoran.delifast.support.SeguridadTest.gestor;
import static dev.carlosmoran.delifast.support.SeguridadTest.token;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.carlosmoran.delifast.domain.Rol;
import dev.carlosmoran.delifast.dto.negocio.AltaMiembroRequest;
import dev.carlosmoran.delifast.dto.negocio.EmpleadoNegocioResponse;
import dev.carlosmoran.delifast.dto.negocio.RepartidorNegocioResponse;
import dev.carlosmoran.delifast.exception.ConflictoException;
import dev.carlosmoran.delifast.exception.ReglaNegocioException;
import dev.carlosmoran.delifast.security.UsuarioAutenticado;
import dev.carlosmoran.delifast.service.EquipoService;
import dev.carlosmoran.delifast.support.SeguridadTest.ConSeguridad;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EquipoController.class)
@ConSeguridad
class EquipoControllerTest {

    private static final UsuarioAutenticado DUENO = new UsuarioAutenticado(GESTOR_ID, Rol.NEGOCIO, 7L, true);
    private static final UUID ANA = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID MARTA = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
    private static final String ALTA = "{\"nombre\":\"Ana Moreno\",\"telefono\":\"655333444\",\"email\":\"ana@lasirena.es\"}";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    EquipoService equipo;

    @Test
    void empleados() throws Exception {
        when(equipo.empleados(DUENO)).thenReturn(List.of(
                new EmpleadoNegocioResponse(GESTOR_ID, "Lola", "lola@lasirena.es", null, true, true)));

        mvc.perform(get("/negocio/empleados").with(gestor(7)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].esDueno").value(true));
    }

    @Test
    void altaEmpleadoDevuelve201() throws Exception {
        when(equipo.altaEmpleado(DUENO, new AltaMiembroRequest("Ana Moreno", "655333444", "ana@lasirena.es")))
                .thenReturn(new EmpleadoNegocioResponse(ANA, "Ana Moreno", "ana@lasirena.es", "655333444", false, true));

        mvc.perform(post("/negocio/empleados").with(gestor(7)).contentType(MediaType.APPLICATION_JSON).content(ALTA))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/negocio/empleados/" + ANA));
    }

    @Test
    void altaPorQuienNoEsDuenoDevuelve403() throws Exception {
        UsuarioAutenticado empleado = new UsuarioAutenticado(ANA, Rol.NEGOCIO, 7L, false);
        when(equipo.altaRepartidor(eq(empleado), any()))
                .thenThrow(new AccessDeniedException("Solo el dueño del negocio puede dar de alta cuentas"));

        mvc.perform(post("/negocio/repartidores").with(token(ANA, "NEGOCIO", 7L, false))
                        .contentType(MediaType.APPLICATION_JSON).content(ALTA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Solo el dueño del negocio puede dar de alta cuentas"));
    }

    @Test
    void altaConEmailRepetidoDevuelve409() throws Exception {
        when(equipo.altaRepartidor(eq(DUENO), any())).thenThrow(new ConflictoException("Ya existe una cuenta con ese email"));

        mvc.perform(post("/negocio/repartidores").with(gestor(7)).contentType(MediaType.APPLICATION_JSON).content(ALTA))
                .andExpect(status().isConflict());
    }

    @Test
    void altaValidaEmail() throws Exception {
        mvc.perform(post("/negocio/empleados").with(gestor(7)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"email\":\"no-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0]").value(org.hamcrest.Matchers.startsWith("email: ")));
        verifyNoInteractions(equipo);
    }

    @Test
    void desactivarAlDuenoDevuelve422() throws Exception {
        when(equipo.cambiarActivoEmpleado(DUENO, GESTOR_ID, false))
                .thenThrow(new ReglaNegocioException("No se puede desactivar al dueño del negocio"));

        mvc.perform(patch("/negocio/empleados/" + GESTOR_ID + "/activo").with(gestor(7))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}"))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void idQueNoEsUuidDevuelve400() throws Exception {
        mvc.perform(put("/negocio/repartidores/42").with(gestor(7)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Marta\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0]").value("id: valor no válido"));
    }

    @Test
    void repartidoresConRepartosDeHoy() throws Exception {
        when(equipo.repartidores(DUENO, true)).thenReturn(List.of(
                new RepartidorNegocioResponse(MARTA, "Marta Díaz", "marta@lasirena.es", null, true, true, 1, 4)));

        mvc.perform(get("/negocio/repartidores?disponible=true").with(gestor(7)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pedidosEnReparto").value(1))
                .andExpect(jsonPath("$[0].repartosHoy").value(4));
    }

    @Test
    void editarYDesactivarRepartidor() throws Exception {
        RepartidorNegocioResponse marta = new RepartidorNegocioResponse(MARTA, "Marta", "m@x.es", null, false, false, 0, 0);
        when(equipo.editarRepartidor(eq(DUENO), eq(MARTA), any())).thenReturn(marta);
        when(equipo.cambiarActivoRepartidor(DUENO, MARTA, false)).thenReturn(marta);

        mvc.perform(put("/negocio/repartidores/" + MARTA).with(gestor(7)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Marta\"}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/negocio/repartidores/" + MARTA + "/activo").with(gestor(7))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }
}
