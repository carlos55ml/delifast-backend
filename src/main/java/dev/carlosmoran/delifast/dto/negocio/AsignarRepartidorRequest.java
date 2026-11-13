package dev.carlosmoran.delifast.dto.negocio;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AsignarRepartidorRequest(@NotNull UUID repartidorId) {
}
