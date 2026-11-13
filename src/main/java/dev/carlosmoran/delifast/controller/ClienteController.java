package dev.carlosmoran.delifast.controller;

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

    @GetMapping("/negocios")
    public List<NegocioClienteResponse> negocios(@RequestParam(required = false) Long localidadId) {
        return catalogo.negocios(localidadId);
    }

    @GetMapping("/negocios/{id}/carta")
    public CartaClienteResponse carta(@PathVariable Long id) {
        return catalogo.carta(id);
    }

    @GetMapping("/me/direcciones")
    public List<DireccionResponse> direcciones(@AuthenticationPrincipal Jwt jwt) {
        return direcciones.listar(UsuarioAutenticado.desde(jwt));
    }

    @PostMapping("/me/direcciones")
    public ResponseEntity<DireccionResponse> crearDireccion(@AuthenticationPrincipal Jwt jwt,
                                                            @Valid @RequestBody DatosDireccionRequest datos) {
        DireccionResponse creada = direcciones.crear(UsuarioAutenticado.desde(jwt), datos);
        return ResponseEntity.created(URI.create("/me/direcciones/" + creada.id())).body(creada);
    }

    @PutMapping("/me/direcciones/{id}")
    public DireccionResponse editarDireccion(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                             @Valid @RequestBody DatosDireccionRequest datos) {
        return direcciones.editar(UsuarioAutenticado.desde(jwt), id, datos);
    }

    @DeleteMapping("/me/direcciones/{id}")
    public ResponseEntity<Void> borrarDireccion(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        direcciones.borrar(UsuarioAutenticado.desde(jwt), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pedidos")
    public List<PedidoClienteResponse> pedidos(@AuthenticationPrincipal Jwt jwt) {
        return pedidos.listar(UsuarioAutenticado.desde(jwt));
    }

    @PostMapping("/pedidos")
    public ResponseEntity<PedidoClienteResponse> crearPedido(@AuthenticationPrincipal Jwt jwt,
                                                             @Valid @RequestBody NuevoPedidoRequest datos) {
        PedidoClienteResponse creado = pedidos.crear(UsuarioAutenticado.desde(jwt), datos);
        return ResponseEntity.created(URI.create("/pedidos/" + creado.id())).body(creado);
    }

    @GetMapping("/pedidos/{id}")
    public PedidoClienteResponse pedido(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return pedidos.obtener(UsuarioAutenticado.desde(jwt), id);
    }
}
