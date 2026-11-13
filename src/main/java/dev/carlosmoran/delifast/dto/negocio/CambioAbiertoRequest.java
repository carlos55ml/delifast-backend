package dev.carlosmoran.delifast.dto.negocio;

import jakarta.validation.constraints.NotNull;

public record CambioAbiertoRequest(@NotNull Boolean abierto) {
}
