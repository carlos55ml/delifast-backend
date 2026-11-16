package dev.carlosmoran.delifast.controller;

import io.swagger.v3.oas.annotations.Operation;
import dev.carlosmoran.delifast.domain.EstadoPedido;
import dev.carlosmoran.delifast.dto.negocio.IncidenciaNegocioResponse;
import dev.carlosmoran.delifast.dto.repartidor.DisponibleRequest;
import dev.carlosmoran.delifast.dto.repartidor.EntregarRequest;
import dev.carlosmoran.delifast.dto.repartidor.IncidenciaRepartoRequest;
import dev.carlosmoran.delifast.dto.repartidor.PerfilRepartidorResponse;
import dev.carlosmoran.delifast.dto.repartidor.RepartoRepartidorResponse;
import dev.carlosmoran.delifast.exception.ValidacionException;
import dev.carlosmoran.delifast.security.UsuarioAutenticado;
import dev.carlosmoran.delifast.service.RepartidorService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Vista del repartidor (R01..R06). Solo rol REPARTIDOR (SecurityConfig). */
@RestController
public class RepartidorController {

    private final RepartidorService servicio;

    public RepartidorController(RepartidorService servicio) {
        this.servicio = servicio;
    }

    @Operation(operationId = "obtenerPerfilRepartidor", summary = "Perfil del repartidor con su disponibilidad y su actividad de hoy (R06)", tags = "Repartidor")
    @GetMapping("/repartidor/perfil")
    public PerfilRepartidorResponse perfil(@AuthenticationPrincipal Jwt jwt) {
        return servicio.perfil(UsuarioAutenticado.desde(jwt));
    }

    @Operation(operationId = "cambiarDisponibleRepartidor", summary = "Marcarse como disponible o no para repartos (R06, arquitectura 5.4)", tags = "Repartidor")
    @PatchMapping("/repartidor/disponible")
    public PerfilRepartidorResponse disponible(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody DisponibleRequest datos) {
        return servicio.cambiarDisponible(UsuarioAutenticado.desde(jwt), datos.disponible());
    }

    @Operation(operationId = "listarRepartosRepartidor", summary = "Mis pedidos (R01 y R05, RF-23)", tags = "Repartidor")
    @GetMapping("/repartidor/repartos")
    public List<RepartoRepartidorResponse> repartos(@AuthenticationPrincipal Jwt jwt,
                                                    @RequestParam(required = false) EstadoPedido estado) {
        if (estado != null && estado != EstadoPedido.EN_REPARTO && estado != EstadoPedido.ENTREGADO) {
            throw new ValidacionException("estado", "solo admite EN_REPARTO o ENTREGADO");
        }
        return servicio.repartos(UsuarioAutenticado.desde(jwt), estado);
    }

    @Operation(operationId = "obtenerRepartoRepartidor", summary = "Detalle de un reparto (R02, RF-24)", tags = "Repartidor")
    @GetMapping("/repartidor/repartos/{id}")
    public RepartoRepartidorResponse reparto(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return servicio.reparto(UsuarioAutenticado.desde(jwt), id);
    }

    @Operation(operationId = "entregarReparto", summary = "Confirmar entrega y cobro (R03, RF-25)", tags = "Repartidor")
    @PostMapping("/repartos/{id}/entregar")
    public RepartoRepartidorResponse entregar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                              @Valid @RequestBody EntregarRequest datos) {
        return servicio.entregar(UsuarioAutenticado.desde(jwt), id, datos.metodoCobro());
    }

    @Operation(operationId = "comunicarIncidenciaReparto", summary = "Comunicar una incidencia de reparto (R04, RF-26)", tags = "Repartidor")
    @PostMapping("/repartos/{id}/incidencias")
    @ResponseStatus(HttpStatus.CREATED)
    public IncidenciaNegocioResponse incidencia(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                                @Valid @RequestBody IncidenciaRepartoRequest datos) {
        return servicio.comunicarIncidencia(UsuarioAutenticado.desde(jwt), id, datos.tipo(), datos.descripcion());
    }
}
