package dev.carlosmoran.delifast.dto.cliente;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DatosDireccionRequest(
        @NotBlank @Size(max = 40) String alias,
        @NotBlank @Size(max = 200) String calle,
        @NotNull @Pattern(regexp = "^\\d{5}$", message = "debe tener 5 dígitos") String codigoPostal,
        @NotNull Long localidadId,
        @Size(max = 200) String indicaciones,
        Boolean predeterminada) {
}
