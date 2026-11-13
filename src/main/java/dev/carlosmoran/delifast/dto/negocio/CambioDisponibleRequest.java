package dev.carlosmoran.delifast.dto.negocio;

import jakarta.validation.constraints.NotNull;

public record CambioDisponibleRequest(@NotNull Boolean disponible) {
}
