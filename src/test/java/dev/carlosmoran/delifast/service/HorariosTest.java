package dev.carlosmoran.delifast.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.carlosmoran.delifast.dto.negocio.FranjaHorarioDto;
import dev.carlosmoran.delifast.exception.ValidacionException;
import java.util.List;
import org.junit.jupiter.api.Test;

class HorariosTest {

    private static FranjaHorarioDto f(int dia, String apertura, String cierre) {
        return new FranjaHorarioDto(dia, apertura, cierre);
    }

    @Test
    void dosFranjasSeparadasEnElMismoDia() {
        assertThatCode(() -> MiNegocioService.validarHorarios(List.of(
                f(1, "12:00", "16:00"), f(1, "20:00", "00:30"), f(2, "12:00", "16:00")))).doesNotThrowAnyException();
    }

    @Test
    void franjasQueSeTocanNoSeSolapan() {
        assertThatCode(() -> MiNegocioService.validarHorarios(List.of(f(1, "12:00", "16:00"), f(1, "16:00", "18:00"))))
                .doesNotThrowAnyException();
    }

    @Test
    void solapeEnElMismoDia() {
        assertThatThrownBy(() -> MiNegocioService.validarHorarios(List.of(f(1, "12:00", "16:00"), f(1, "15:00", "18:00"))))
                .isInstanceOf(ValidacionException.class)
                .satisfies(e -> org.assertj.core.api.Assertions.assertThat(((ValidacionException) e).getErrores())
                        .containsExactly("horarios[1]: se solapa con la franja 1 (12:00-16:00)"));
    }

    @Test
    void franjaDeMedianocheChocaConLaMananaSiguiente() {
        assertThatThrownBy(() -> MiNegocioService.validarHorarios(List.of(f(1, "20:00", "02:00"), f(2, "01:00", "03:00"))))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void domingoNocheChocaConLunes() {
        assertThatThrownBy(() -> MiNegocioService.validarHorarios(List.of(f(7, "23:00", "01:00"), f(1, "00:30", "02:00"))))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void aperturaIgualACierre() {
        assertThatThrownBy(() -> MiNegocioService.validarHorarios(List.of(f(3, "12:00", "12:00"))))
                .isInstanceOf(ValidacionException.class);
    }
}
