package dev.carlosmoran.delifast.dto.negocio;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Parte "datos" de PUT /negocio. La localidad no se cambia aquí. */
public record DatosMiNegocioRequest(
        @NotBlank @Size(max = 100) String nombre,
        @Size(max = 300) String descripcion,
        @NotBlank @Size(min = 9, max = 20) String telefono,
        @NotBlank @Size(max = 200) String direccion,
        @NotNull @DecimalMin("0") @DecimalMax("50") @Digits(integer = 2, fraction = 2) BigDecimal gastosEnvio,
        @NotNull @DecimalMin("0") @DecimalMax("200") @Digits(integer = 3, fraction = 2) BigDecimal pedidoMinimo) {
}
