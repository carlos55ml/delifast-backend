package dev.carlosmoran.delifast.controller;

import io.swagger.v3.oas.annotations.Operation;
import dev.carlosmoran.delifast.dto.cliente.CartaClienteResponse;
import dev.carlosmoran.delifast.dto.cliente.DatosDireccionRequest;
import dev.carlosmoran.delifast.dto.cliente.DireccionResponse;
import dev.carlosmoran.delifast.dto.cliente.NegocioClienteResponse;
import dev.carlosmoran.delifast.dto.cliente.NuevoPedidoRequest;
import dev.carlosmoran.delifast.dto.cliente.PedidoClienteResponse;
import dev.carlosmoran.delifast.security.UsuarioAutenticado;
import dev.carlosmoran.delifast.service.CatalogoClienteService;
import dev.carlosmoran.delifast.service.DireccionService;
import dev.carlosmoran.delifast.service.PedidoClienteService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * App del cliente: negocios y carta (públicos), direcciones y pedidos (rol CLIENTE, SecurityConfig).
 */
@RestController
public class ClienteController {

    private final CatalogoClienteService catalogo;
    private final DireccionService direcciones;
    private final PedidoClienteService pedidos;

    public ClienteController(CatalogoClienteService catalogo, DireccionService direcciones,
                             PedidoClienteService pedidos) {
        this.catalogo = catalogo;
        this.direcciones = direcciones;
        this.pedidos = pedidos;
    }

    @Operation(operationId = "listarNegocios", summary = "Negocios activos para el cliente (RF-05)", tags = "Cliente · Negocios")
    @GetMapping("/negocios")
    public List<NegocioClienteResponse> negocios(@RequestParam(required = false) Long localidadId) {
        return catalogo.negocios(localidadId);
    }

    @Operation(operationId = "obtenerCartaCliente", summary = "Carta de un negocio para el cliente (RF-06)", tags = "Cliente · Negocios")
    @GetMapping("/negocios/{id}/carta")
    public CartaClienteResponse carta(@PathVariable Long id) {
        return catalogo.carta(id);
    }

    @Operation(operationId = "listarDirecciones", summary = "Direcciones del cliente (RF-04)", tags = "Cliente · Direcciones")
    @GetMapping("/me/direcciones")
    public List<DireccionResponse> direcciones(@AuthenticationPrincipal Jwt jwt) {
        return direcciones.listar(UsuarioAutenticado.desde(jwt));
    }

    @Operation(operationId = "crearDireccion", summary = "Añadir una dirección (RF-04)", tags = "Cliente · Direcciones")
    @PostMapping("/me/direcciones")
    public ResponseEntity<DireccionResponse> crearDireccion(@AuthenticationPrincipal Jwt jwt,
                                                            @Valid @RequestBody DatosDireccionRequest datos) {
        DireccionResponse creada = direcciones.crear(UsuarioAutenticado.desde(jwt), datos);
        return ResponseEntity.created(URI.create("/me/direcciones/" + creada.id())).body(creada);
    }

    @Operation(operationId = "editarDireccion", summary = "Editar una dirección (o marcarla como predeterminada)", tags = "Cliente · Direcciones")
    @PutMapping("/me/direcciones/{id}")
    public DireccionResponse editarDireccion(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                             @Valid @RequestBody DatosDireccionRequest datos) {
        return direcciones.editar(UsuarioAutenticado.desde(jwt), id, datos);
    }

    @Operation(operationId = "borrarDireccion", summary = "Borrar una dirección", tags = "Cliente · Direcciones")
    @DeleteMapping("/me/direcciones/{id}")
    public ResponseEntity<Void> borrarDireccion(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        direcciones.borrar(UsuarioAutenticado.desde(jwt), id);
        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "listarMisPedidos", summary = "Pedidos del cliente, del más reciente al más antiguo (RF-10)", tags = "Cliente · Pedidos")
    @GetMapping("/pedidos")
    public List<PedidoClienteResponse> pedidos(@AuthenticationPrincipal Jwt jwt) {
        return pedidos.listar(UsuarioAutenticado.desde(jwt));
    }

    @Operation(operationId = "crearPedido", summary = "Hacer un pedido (RF-08)", tags = "Cliente · Pedidos")
    @PostMapping("/pedidos")
    public ResponseEntity<PedidoClienteResponse> crearPedido(@AuthenticationPrincipal Jwt jwt,
                                                             @Valid @RequestBody NuevoPedidoRequest datos) {
        PedidoClienteResponse creado = pedidos.crear(UsuarioAutenticado.desde(jwt), datos);
        return ResponseEntity.created(URI.create("/pedidos/" + creado.id())).body(creado);
    }

    @Operation(operationId = "obtenerMiPedido", summary = "Un pedido del cliente con su historial (seguimiento, RF-09)", tags = "Cliente · Pedidos")
    @GetMapping("/pedidos/{id}")
    public PedidoClienteResponse pedido(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return pedidos.obtener(UsuarioAutenticado.desde(jwt), id);
    }
}
