package dev.carlosmoran.delifast.dto.repartidor;

import dev.carlosmoran.delifast.domain.TipoIncidencia;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record IncidenciaRepartoRequest(@NotNull TipoIncidencia tipo, @Size(max = 500) String descripcion) {
}
