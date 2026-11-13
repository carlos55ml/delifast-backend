package dev.carlosmoran.delifast.dto.admin;

import jakarta.validation.constraints.NotNull;

public record CambioActivoRequest(@NotNull Boolean activo) {
}
