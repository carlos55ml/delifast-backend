package dev.carlosmoran.delifast.dto.negocio;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AceptarPedidoRequest(@NotNull @Min(5) @Max(120) Integer tiempoEstimadoMin) {
}
