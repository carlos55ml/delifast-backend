package dev.carlosmoran.delifast.controller;

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
    @GetMapping("/localidades")
    public List<LocalidadResponse> listar() {
        return localidades.listar();
    }
}
