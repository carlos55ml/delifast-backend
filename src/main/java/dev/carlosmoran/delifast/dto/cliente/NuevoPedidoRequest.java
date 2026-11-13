package dev.carlosmoran.delifast.dto.cliente;

import dev.carlosmoran.delifast.domain.MetodoPago;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Cuerpo de POST /pedidos. Precios y total los calcula el backend. */
public record NuevoPedidoRequest(
        @NotNull Long negocioId,
        @NotNull Long direccionId,
        @NotNull MetodoPago metodoPago,
        @Size(max = 500) String nota,
        @NotEmpty @Size(max = 50) List<@Valid @NotNull Linea> lineas) {

    public record Linea(@NotNull Long productoId, @NotNull @Min(1) @Max(50) Integer cantidad) {
    }
}
