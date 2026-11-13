package dev.carlosmoran.delifast.dto.repartidor;

import jakarta.validation.constraints.NotNull;

public record DisponibleRequest(@NotNull Boolean disponible) {
}
