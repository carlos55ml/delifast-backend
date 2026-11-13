package dev.carlosmoran.delifast.controller;

import dev.carlosmoran.delifast.domain.EstadoIncidencia;
import dev.carlosmoran.delifast.dto.negocio.EstadisticasNegocioResponse;
import dev.carlosmoran.delifast.dto.negocio.IncidenciaNegocioResponse;
import dev.carlosmoran.delifast.dto.negocio.Periodo;
import dev.carlosmoran.delifast.dto.negocio.ResolverIncidenciaRequest;
import dev.carlosmoran.delifast.dto.negocio.RespuestaIncidenciaRequest;
import dev.carlosmoran.delifast.security.UsuarioAutenticado;
import dev.carlosmoran.delifast.service.EstadisticasService;
import dev.carlosmoran.delifast.service.IncidenciaNegocioService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Incidencias (N05, RF-21) y estadísticas (N06, RF-22) del negocio. Solo rol NEGOCIO. */
@RestController
public class IncidenciaNegocioController {

    private final IncidenciaNegocioService incidencias;
    private final EstadisticasService estadisticas;

    public IncidenciaNegocioController(IncidenciaNegocioService incidencias, EstadisticasService estadisticas) {
        this.incidencias = incidencias;
        this.estadisticas = estadisticas;
    }

    @GetMapping("/negocio/incidencias")
    public List<IncidenciaNegocioResponse> listar(@AuthenticationPrincipal Jwt jwt,
                                                  @RequestParam(required = false) List<EstadoIncidencia> estado) {
        return incidencias.listar(UsuarioAutenticado.desde(jwt), estado);
    }

    @PostMapping("/incidencias/{id}/responder")
    public IncidenciaNegocioResponse responder(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                               @Valid @RequestBody RespuestaIncidenciaRequest datos) {
        return incidencias.responder(UsuarioAutenticado.desde(jwt), id, datos.respuesta());
    }

    @PostMapping("/incidencias/{id}/resolver")
    public IncidenciaNegocioResponse resolver(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                              @Valid @RequestBody(required = false) ResolverIncidenciaRequest datos) {
        return incidencias.resolver(UsuarioAutenticado.desde(jwt), id, datos == null ? null : datos.respuesta());
    }

    @GetMapping("/negocio/estadisticas")
    public EstadisticasNegocioResponse estadisticas(@AuthenticationPrincipal Jwt jwt,
                                                    @RequestParam Periodo periodo,
                                                    @RequestParam(required = false) LocalDate fecha) {
        return estadisticas.calcular(UsuarioAutenticado.desde(jwt), periodo, fecha);
    }
}
