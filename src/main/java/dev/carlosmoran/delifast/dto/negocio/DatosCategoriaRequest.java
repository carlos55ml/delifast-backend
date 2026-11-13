package dev.carlosmoran.delifast.dto.negocio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DatosCategoriaRequest(@NotBlank @Size(max = 60) String nombre) {
}
