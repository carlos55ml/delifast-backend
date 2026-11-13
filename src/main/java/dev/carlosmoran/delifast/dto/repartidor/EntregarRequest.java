package dev.carlosmoran.delifast.dto.repartidor;

import dev.carlosmoran.delifast.domain.MetodoPago;
import jakarta.validation.constraints.NotNull;

public record EntregarRequest(@NotNull MetodoPago metodoCobro) {
}
