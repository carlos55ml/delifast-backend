package dev.carlosmoran.delifast.controller;

import io.swagger.v3.oas.annotations.Operation;
import dev.carlosmoran.delifast.dto.LocalidadResponse;
import dev.carlosmoran.delifast.service.LocalidadService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LocalidadController {

    private final LocalidadService localidades;

    public LocalidadController(LocalidadService localidades) {
        this.localidades = localidades;
    }

    /** Pública: también la usa la app al registrarse. */
    @Operation(operationId = "listarLocalidades", summary = "Localidades donde opera DeliFast", tags = "Localidades")
    @GetMapping("/localidades")
    public List<LocalidadResponse> listar() {
        return localidades.listar();
    }
}
