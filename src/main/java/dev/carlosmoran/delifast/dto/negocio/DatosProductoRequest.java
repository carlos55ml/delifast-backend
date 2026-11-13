package dev.carlosmoran.delifast.dto.negocio;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Parte "datos" del formulario de producto. quitarFoto solo se usa al editar. */
public record DatosProductoRequest(
        @NotNull Long categoriaId,
        @NotBlank @Size(max = 100) String nombre,
        @Size(max = 300) String descripcion,
        @NotNull @DecimalMin("0") @DecimalMax("999.99") @Digits(integer = 3, fraction = 2) BigDecimal precio,
        @NotNull Boolean disponible,
        Boolean quitarFoto) {
}
