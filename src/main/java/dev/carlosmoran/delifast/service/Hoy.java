package dev.carlosmoran.delifast.service;

import dev.carlosmoran.delifast.config.ClockConfig;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/** Fechas de "hoy" en Europe/Madrid a partir del reloj de la aplicación. */
final class Hoy {

    private Hoy() {
    }

    static OffsetDateTime inicio(Clock clock) {
        return LocalDate.now(clock.withZone(ClockConfig.ZONA)).atStartOfDay(ClockConfig.ZONA).toOffsetDateTime();
    }

    static LocalDateTime ahoraLocal(Clock clock) {
        return LocalDateTime.now(clock.withZone(ClockConfig.ZONA));
    }
}
