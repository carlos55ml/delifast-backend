package dev.carlosmoran.delifast.controller;

import io.swagger.v3.oas.annotations.Operation;
import dev.carlosmoran.delifast.dto.negocio.CambioDisponibleRequest;
import dev.carlosmoran.delifast.dto.negocio.CategoriaCartaResponse;
import dev.carlosmoran.delifast.dto.negocio.DatosCategoriaRequest;
import dev.carlosmoran.delifast.dto.negocio.DatosProductoRequest;
import dev.carlosmoran.delifast.dto.negocio.ProductoCartaResponse;
import dev.carlosmoran.delifast.security.UsuarioAutenticado;
import dev.carlosmoran.delifast.service.CartaService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Carta del negocio (N03, RF-17). Solo rol NEGOCIO (SecurityConfig). */
@RestController
@RequestMapping("/negocio")
public class CartaController {

    private final CartaService carta;

    public CartaController(CartaService carta) {
        this.carta = carta;
    }

    @Operation(operationId = "obtenerCartaNegocio", summary = "Carta completa del negocio (N03)", tags = "Negocio · Carta")
    @GetMapping("/carta")
    public List<CategoriaCartaResponse> carta(@AuthenticationPrincipal Jwt jwt) {
        return carta.carta(UsuarioAutenticado.desde(jwt));
    }

    @Operation(operationId = "crearCategoria", summary = "Nueva categoría (se añade al final)", tags = "Negocio · Carta")
    @PostMapping("/categorias")
    public ResponseEntity<CategoriaCartaResponse> crearCategoria(@AuthenticationPrincipal Jwt jwt,
                                                                 @Valid @RequestBody DatosCategoriaRequest datos) {
        CategoriaCartaResponse creada = carta.crearCategoria(UsuarioAutenticado.desde(jwt), datos.nombre());
        return ResponseEntity.created(URI.create("/negocio/categorias/" + creada.id())).body(creada);
    }

    @Operation(operationId = "editarCategoria", summary = "Renombrar una categoría", tags = "Negocio · Carta")
    @PutMapping("/categorias/{id}")
    public CategoriaCartaResponse renombrarCategoria(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                                     @Valid @RequestBody DatosCategoriaRequest datos) {
        return carta.renombrarCategoria(UsuarioAutenticado.desde(jwt), id, datos.nombre());
    }

    @Operation(operationId = "borrarCategoria", summary = "Borrar una categoría vacía", tags = "Negocio · Carta")
    @DeleteMapping("/categorias/{id}")
    public ResponseEntity<Void> borrarCategoria(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        carta.borrarCategoria(UsuarioAutenticado.desde(jwt), id);
        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "crearProducto", summary = "Nuevo producto con foto opcional (N03, RF-17)", tags = "Negocio · Carta")
    @PostMapping(value = "/productos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductoCartaResponse> crearProducto(@AuthenticationPrincipal Jwt jwt,
                                                               @Valid @RequestPart("datos") DatosProductoRequest datos,
                                                               @RequestPart(value = "foto", required = false) MultipartFile foto) {
        ProductoCartaResponse creado = carta.crearProducto(UsuarioAutenticado.desde(jwt), datos, foto);
        return ResponseEntity.created(URI.create("/negocio/productos/" + creado.id())).body(creado);
    }

    @Operation(operationId = "editarProducto", summary = "Editar un producto (y cambiar o quitar su foto)", tags = "Negocio · Carta")
    @PutMapping(value = "/productos/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductoCartaResponse editarProducto(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                                @Valid @RequestPart("datos") DatosProductoRequest datos,
                                                @RequestPart(value = "foto", required = false) MultipartFile foto) {
        return carta.editarProducto(UsuarioAutenticado.desde(jwt), id, datos, foto);
    }

    @Operation(operationId = "borrarProducto", summary = "Borrar un producto", tags = "Negocio · Carta")
    @DeleteMapping("/productos/{id}")
    public ResponseEntity<Void> borrarProducto(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        carta.borrarProducto(UsuarioAutenticado.desde(jwt), id);
        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "cambiarDisponibleProducto", summary = "Marcar un producto como disponible o agotado (interruptor de N03)", tags = "Negocio · Carta")
    @PatchMapping("/productos/{id}/disponible")
    public ProductoCartaResponse cambiarDisponible(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                                   @Valid @RequestBody CambioDisponibleRequest datos) {
        return carta.cambiarDisponible(UsuarioAutenticado.desde(jwt), id, datos.disponible());
    }
}
