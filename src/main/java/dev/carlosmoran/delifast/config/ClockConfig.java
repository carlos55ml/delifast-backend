package dev.carlosmoran.delifast.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj de la aplicación en hora peninsular española: "hoy" en los resúmenes y el horario
 * de los negocios se calculan en Europe/Madrid. Los tests lo sustituyen por un reloj fijo.
 */
@Configuration
public class ClockConfig {

    public static final ZoneId ZONA = ZoneId.of("Europe/Madrid");

    @Bean
    Clock clock() {
        return Clock.system(ZONA);
    }
}
