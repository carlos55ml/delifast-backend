package dev.carlosmoran.delifast.controller;

import io.swagger.v3.oas.annotations.Operation;
import dev.carlosmoran.delifast.dto.admin.AltaNegocioRequest;
import dev.carlosmoran.delifast.dto.admin.CambioActivoRequest;
import dev.carlosmoran.delifast.dto.admin.DatosNegocioRequest;
import dev.carlosmoran.delifast.dto.admin.NegocioAdminResponse;
import dev.carlosmoran.delifast.service.NegocioAdminService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Negocios en la zona de administración (A02, A03). Solo ADMINISTRADOR (SecurityConfig). */
@RestController
@RequestMapping("/admin/negocios")
public class AdminNegocioController {

    private final NegocioAdminService servicio;

    public AdminNegocioController(NegocioAdminService servicio) {
        this.servicio = servicio;
    }

    @Operation(operationId = "listarNegociosAdmin", summary = "Negocios dados de alta (A02, RF-29)", tags = "Administración")
    @GetMapping
    public List<NegocioAdminResponse> listar(@RequestParam(required = false) Boolean activo) {
        return servicio.listar(activo);
    }

    @Operation(operationId = "altaNegocio", summary = "Dar de alta un negocio y la cuenta de su dueño (A03, RF-28)", tags = "Administración")
    @PostMapping
    public ResponseEntity<NegocioAdminResponse> alta(@Valid @RequestBody AltaNegocioRequest datos) {
        NegocioAdminResponse creado = servicio.alta(datos);
        return ResponseEntity.created(URI.create("/admin/negocios/" + creado.id())).body(creado);
    }

    @Operation(operationId = "editarNegocioAdmin", summary = "Corregir los datos básicos de un negocio (icono de editar en A02)", tags = "Administración")
    @PutMapping("/{id}")
    public NegocioAdminResponse editar(@PathVariable Long id, @Valid @RequestBody DatosNegocioRequest datos) {
        return servicio.editar(id, datos);
    }

    @Operation(operationId = "cambiarActivoNegocio", summary = "Activar o suspender un negocio (interruptor de A02, RF-29)", tags = "Administración")
    @PatchMapping("/{id}/activo")
    public NegocioAdminResponse cambiarActivo(@PathVariable Long id, @Valid @RequestBody CambioActivoRequest datos) {
        return servicio.cambiarActivo(id, datos.activo());
    }
}
