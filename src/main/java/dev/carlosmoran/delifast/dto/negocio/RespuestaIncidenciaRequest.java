package dev.carlosmoran.delifast.dto.negocio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RespuestaIncidenciaRequest(@NotBlank @Size(max = 500) String respuesta) {
}
