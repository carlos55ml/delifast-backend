package dev.carlosmoran.delifast.controller;

import dev.carlosmoran.delifast.dto.PerfilResponse;
import dev.carlosmoran.delifast.service.PerfilService;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PerfilController {

    private final PerfilService perfiles;

    public PerfilController(PerfilService perfiles) {
        this.perfiles = perfiles;
    }

    @GetMapping("/me")
    public PerfilResponse perfil(@AuthenticationPrincipal Jwt jwt) {
        return perfiles.obtener(UUID.fromString(jwt.getSubject()));
    }
}
