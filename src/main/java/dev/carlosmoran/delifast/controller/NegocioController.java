package dev.carlosmoran.delifast.controller;

import io.swagger.v3.oas.annotations.Operation;
import dev.carlosmoran.delifast.domain.EstadoPedido;
import dev.carlosmoran.delifast.dto.negocio.AceptarPedidoRequest;
import dev.carlosmoran.delifast.dto.negocio.AsignarRepartidorRequest;
import dev.carlosmoran.delifast.dto.negocio.CambioAbiertoRequest;
import dev.carlosmoran.delifast.dto.negocio.DatosMiNegocioRequest;
import dev.carlosmoran.delifast.dto.negocio.FranjaHorarioDto;
import dev.carlosmoran.delifast.dto.negocio.MiNegocioResponse;
import dev.carlosmoran.delifast.dto.negocio.PedidoNegocioResponse;
import dev.carlosmoran.delifast.dto.negocio.RechazarPedidoRequest;
import dev.carlosmoran.delifast.security.UsuarioAutenticado;
import dev.carlosmoran.delifast.service.MiNegocioService;
import dev.carlosmoran.delifast.service.PedidoNegocioService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Panel del negocio: pedidos (N01, N02, N08) y configuración (N07). Solo rol NEGOCIO (SecurityConfig) y siempre sobre el
 * negocio del claim negocio_id.
 */
@RestController
public class NegocioController {

    private final MiNegocioService miNegocio;
    private final PedidoNegocioService pedidos;

    public NegocioController(MiNegocioService miNegocio, PedidoNegocioService pedidos) {
        this.miNegocio = miNegocio;
        this.pedidos = pedidos;
    }

    @Operation(operationId = "obtenerMiNegocio", summary = "Datos y configuración del negocio del usuario (cabecera del panel y N07)", tags = "Negocio · Configuración")
    @GetMapping("/negocio")
    public MiNegocioResponse negocio(@AuthenticationPrincipal Jwt jwt) {
        return miNegocio.obtener(UsuarioAutenticado.desde(jwt));
    }

    @Operation(operationId = "editarMiNegocio", summary = "Guardar los datos del negocio y, opcionalmente, un logo nuevo (N07, RF-18)", tags = "Negocio · Configuración")
    @PutMapping(value = "/negocio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MiNegocioResponse editar(@AuthenticationPrincipal Jwt jwt,
                                    @Valid @RequestPart("datos") DatosMiNegocioRequest datos,
                                    @RequestPart(value = "logo", required = false) MultipartFile logo) {
        return miNegocio.editar(UsuarioAutenticado.desde(jwt), datos, logo);
    }

    @Operation(operationId = "editarHorariosNegocio", summary = "Sustituir el horario de apertura completo (N07, RF-18)", tags = "Negocio · Configuración")
    @PutMapping("/negocio/horarios")
    public MiNegocioResponse horarios(@AuthenticationPrincipal Jwt jwt,
                                      @RequestBody @Size(max = 21) List<@Valid FranjaHorarioDto> horarios) {
        return miNegocio.cambiarHorarios(UsuarioAutenticado.desde(jwt), horarios);
    }

    @Operation(operationId = "cambiarAbiertoNegocio", summary = "Abrir o cerrar el negocio a mano (interruptor \"Abierto\", RF-18)", tags = "Negocio · Configuración")
    @PatchMapping("/negocio/abierto")
    public MiNegocioResponse cambiarAbierto(@AuthenticationPrincipal Jwt jwt,
                                            @Valid @RequestBody CambioAbiertoRequest datos) {
        return miNegocio.cambiarAbierto(UsuarioAutenticado.desde(jwt), datos.abierto());
    }

    @Operation(operationId = "listarPedidosNegocio", summary = "Pedidos del negocio (tablero N01, RF-14)", tags = "Negocio · Pedidos")
    @GetMapping("/negocio/pedidos")
    public List<PedidoNegocioResponse> pedidos(@AuthenticationPrincipal Jwt jwt,
                                               @RequestParam(required = false) List<EstadoPedido> estado) {
        return pedidos.listar(UsuarioAutenticado.desde(jwt), estado);
    }

    @Operation(operationId = "obtenerPedidoNegocio", summary = "Detalle de un pedido con su historial de estados (N02, RF-16)", tags = "Negocio · Pedidos")
    @GetMapping("/negocio/pedidos/{id}")
    public PedidoNegocioResponse pedido(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return pedidos.obtener(UsuarioAutenticado.desde(jwt), id);
    }

    @Operation(operationId = "aceptarPedido", summary = "Aceptar un pedido con tiempo estimado (RECIBIDO → EN_PREPARACION, RF-15)", tags = "Negocio · Pedidos")
    @PostMapping("/pedidos/{id}/aceptar")
    public PedidoNegocioResponse aceptar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                         @Valid @RequestBody AceptarPedidoRequest datos) {
        return pedidos.aceptar(UsuarioAutenticado.desde(jwt), id, datos.tiempoEstimadoMin());
    }

    @Operation(operationId = "rechazarPedido", summary = "Rechazar un pedido con motivo (RECIBIDO → RECHAZADO, RF-15)", tags = "Negocio · Pedidos")
    @PostMapping("/pedidos/{id}/rechazar")
    public PedidoNegocioResponse rechazar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                          @Valid @RequestBody RechazarPedidoRequest datos) {
        return pedidos.rechazar(UsuarioAutenticado.desde(jwt), id, datos.motivo());
    }

    @Operation(operationId = "marcarPedidoListo", summary = "Marcar como listo (EN_PREPARACION → LISTO, RF-16)", tags = "Negocio · Pedidos")
    @PostMapping("/pedidos/{id}/listo")
    public PedidoNegocioResponse listo(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return pedidos.marcarListo(UsuarioAutenticado.desde(jwt), id);
    }

    @Operation(operationId = "asignarRepartidor", summary = "Asignar un repartidor a un pedido listo (LISTO → EN_REPARTO, N08, RF-20)", tags = "Negocio · Pedidos")
    @PostMapping("/pedidos/{id}/asignar")
    public PedidoNegocioResponse asignar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                         @Valid @RequestBody AsignarRepartidorRequest datos) {
        return pedidos.asignar(UsuarioAutenticado.desde(jwt), id, datos.repartidorId());
    }
}
