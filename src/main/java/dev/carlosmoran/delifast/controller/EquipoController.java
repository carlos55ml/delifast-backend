package dev.carlosmoran.delifast.controller;

import dev.carlosmoran.delifast.dto.admin.CambioActivoRequest;
import dev.carlosmoran.delifast.dto.negocio.AltaMiembroRequest;
import dev.carlosmoran.delifast.dto.negocio.DatosMiembroRequest;
import dev.carlosmoran.delifast.dto.negocio.EmpleadoNegocioResponse;
import dev.carlosmoran.delifast.dto.negocio.RepartidorNegocioResponse;
import dev.carlosmoran.delifast.security.UsuarioAutenticado;
import dev.carlosmoran.delifast.service.EquipoService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Equipo del negocio (N04, RF-19). Solo rol NEGOCIO; las altas, solo el dueño. */
@RestController
@RequestMapping("/negocio")
public class EquipoController {

    private final EquipoService equipo;

    public EquipoController(EquipoService equipo) {
        this.equipo = equipo;
    }

    @GetMapping("/empleados")
    public List<EmpleadoNegocioResponse> empleados(@AuthenticationPrincipal Jwt jwt) {
        return equipo.empleados(UsuarioAutenticado.desde(jwt));
    }

    @PostMapping("/empleados")
    public ResponseEntity<EmpleadoNegocioResponse> altaEmpleado(@AuthenticationPrincipal Jwt jwt,
                                                                @Valid @RequestBody AltaMiembroRequest datos) {
        EmpleadoNegocioResponse creado = equipo.altaEmpleado(UsuarioAutenticado.desde(jwt), datos);
        return ResponseEntity.created(URI.create("/negocio/empleados/" + creado.id())).body(creado);
    }

    @PutMapping("/empleados/{id}")
    public EmpleadoNegocioResponse editarEmpleado(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                                  @Valid @RequestBody DatosMiembroRequest datos) {
        return equipo.editarEmpleado(UsuarioAutenticado.desde(jwt), id, datos);
    }

    @PatchMapping("/empleados/{id}/activo")
    public EmpleadoNegocioResponse activoEmpleado(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                                  @Valid @RequestBody CambioActivoRequest datos) {
        return equipo.cambiarActivoEmpleado(UsuarioAutenticado.desde(jwt), id, datos.activo());
    }

    @GetMapping("/repartidores")
    public List<RepartidorNegocioResponse> repartidores(@AuthenticationPrincipal Jwt jwt,
                                                        @RequestParam(required = false) Boolean disponible) {
        return equipo.repartidores(UsuarioAutenticado.desde(jwt), disponible);
    }

    @PostMapping("/repartidores")
    public ResponseEntity<RepartidorNegocioResponse> altaRepartidor(@AuthenticationPrincipal Jwt jwt,
                                                                    @Valid @RequestBody AltaMiembroRequest datos) {
        RepartidorNegocioResponse creado = equipo.altaRepartidor(UsuarioAutenticado.desde(jwt), datos);
        return ResponseEntity.created(URI.create("/negocio/repartidores/" + creado.id())).body(creado);
    }

    @PutMapping("/repartidores/{id}")
    public RepartidorNegocioResponse editarRepartidor(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                                      @Valid @RequestBody DatosMiembroRequest datos) {
        return equipo.editarRepartidor(UsuarioAutenticado.desde(jwt), id, datos);
    }

    @PatchMapping("/repartidores/{id}/activo")
    public RepartidorNegocioResponse activoRepartidor(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                                      @Valid @RequestBody CambioActivoRequest datos) {
        return equipo.cambiarActivoRepartidor(UsuarioAutenticado.desde(jwt), id, datos.activo());
    }
}
