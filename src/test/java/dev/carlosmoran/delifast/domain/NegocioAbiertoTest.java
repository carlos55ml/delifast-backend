package dev.carlosmoran.delifast.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class NegocioAbiertoTest {

    // 2026-11-16 es lunes.
    private static final LocalDateTime LUNES_21H = LocalDateTime.of(2026, 11, 16, 21, 0);
    private static final LocalDateTime MARTES_0015 = LocalDateTime.of(2026, 11, 17, 0, 15);
    private static final LocalDateTime MARTES_0100 = LocalDateTime.of(2026, 11, 17, 1, 0);
    private static final LocalDateTime LUNES_13H = LocalDateTime.of(2026, 11, 16, 13, 0);

    private static Negocio negocio(boolean interruptor) {
        Negocio n = new Negocio(new Localidad("Osuna", "Sevilla"), "La Sirena", "955000000", "C/ A");
        n.setAbiertoManual(interruptor);
        return n;
    }

    @Test
    void conInterruptorCerradoNoAbre() {
        assertThat(negocio(false).estaAbierto(LUNES_21H)).isFalse();
    }

    @Test
    void sinHorariosMandaElInterruptor() {
        assertThat(negocio(true).estaAbierto(LUNES_21H)).isTrue();
    }

    @Test
    void suspendidoNoAbre() {
        Negocio n = negocio(true);
        n.setActivo(false);
        assertThat(n.estaAbierto(LUNES_21H)).isFalse();
    }

    @Test
    void franjaNormal() {
        Negocio n = negocio(true);
        n.getHorarios().add(new HorarioApertura(n, DiaSemana.LUNES, LocalTime.of(12, 0), LocalTime.of(16, 0)));

        assertThat(n.estaAbierto(LUNES_13H)).isTrue();
        assertThat(n.estaAbierto(LUNES_21H)).isFalse();
        assertThat(n.estaAbierto(LUNES_13H.plusDays(1))).isFalse();
    }

    @Test
    void franjaQuePasaDeMedianoche() {
        Negocio n = negocio(true);
        n.getHorarios().add(new HorarioApertura(n, DiaSemana.LUNES, LocalTime.of(20, 0), LocalTime.of(0, 30)));

        assertThat(n.estaAbierto(LUNES_21H)).isTrue();
        assertThat(n.estaAbierto(MARTES_0015)).isTrue();
        assertThat(n.estaAbierto(MARTES_0100)).isFalse();
        assertThat(n.estaAbierto(LUNES_13H)).isFalse();
    }

    @Test
    void domingoQuePasaALunes() {
        Negocio n = negocio(true);
        n.getHorarios().add(new HorarioApertura(n, DiaSemana.DOMINGO, LocalTime.of(20, 0), LocalTime.of(1, 0)));

        assertThat(n.estaAbierto(LocalDateTime.of(2026, 11, 16, 0, 30))).isTrue();
    }
}
