package dev.carlosmoran.delifast.dto.negocio;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Cuerpo opcional de /resolver: si llega respuesta, no puede estar vacía. */
public record ResolverIncidenciaRequest(
        @Size(min = 1, max = 500) @Pattern(regexp = "(?s).*\\S.*", message = "no puede estar vacía") String respuesta) {
}
