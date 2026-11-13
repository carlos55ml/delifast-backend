package dev.carlosmoran.delifast.dto.negocio;

import dev.carlosmoran.delifast.domain.HorarioApertura;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Franja de apertura: día 1 (lunes) a 7 (domingo) y horas "HH:mm". */
public record FranjaHorarioDto(
        @NotNull @Min(1) @Max(7) Integer dia,
        @NotNull @Pattern(regexp = HORA, message = "debe tener el formato HH:mm") String apertura,
        @NotNull @Pattern(regexp = HORA, message = "debe tener el formato HH:mm") String cierre) {

    static final String HORA = "^([01]\\d|2[0-3]):[0-5]\\d$";
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("HH:mm");

    public static FranjaHorarioDto de(HorarioApertura h) {
        return new FranjaHorarioDto((int) h.getDia().numero(), h.getHoraApertura().format(FORMATO),
                h.getHoraCierre().format(FORMATO));
    }

    public LocalTime horaApertura() {
        return LocalTime.parse(apertura, FORMATO);
    }

    public LocalTime horaCierre() {
        return LocalTime.parse(cierre, FORMATO);
    }
}
