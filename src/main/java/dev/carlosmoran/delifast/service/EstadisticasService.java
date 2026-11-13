package dev.carlosmoran.delifast.service;

import dev.carlosmoran.delifast.config.ClockConfig;
import dev.carlosmoran.delifast.domain.EstadoIncidencia;
import dev.carlosmoran.delifast.dto.negocio.EstadisticasNegocioResponse;
import dev.carlosmoran.delifast.dto.negocio.EstadisticasNegocioResponse.Incidencias;
import dev.carlosmoran.delifast.dto.negocio.EstadisticasNegocioResponse.ProductoVendido;
import dev.carlosmoran.delifast.dto.negocio.EstadisticasNegocioResponse.Tramo;
import dev.carlosmoran.delifast.dto.negocio.Periodo;
import dev.carlosmoran.delifast.repository.IncidenciaRepository;
import dev.carlosmoran.delifast.repository.PedidoRepository;
import dev.carlosmoran.delifast.security.UsuarioAutenticado;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Estadísticas del negocio (N06, RF-22). Periodos en Europe/Madrid. Cuentan los pedidos que no
 * se han rechazado ni cancelado; la facturación suma los ENTREGADO.
 */
@Service
public class EstadisticasService {

    private static final String[] DIAS = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};
    private static final int MAS_VENDIDOS = 5;

    private final NegocioActual negocioActual;
    private final PedidoRepository pedidos;
    private final IncidenciaRepository incidencias;
    private final Clock clock;

    public EstadisticasService(NegocioActual negocioActual, PedidoRepository pedidos, IncidenciaRepository incidencias,
                               Clock clock) {
        this.negocioActual = negocioActual;
        this.pedidos = pedidos;
        this.incidencias = incidencias;
        this.clock = clock;
    }

    /** Días [desde, hasta] del periodo que contiene la fecha. */
    record Rango(LocalDate desde, LocalDate hasta) {

        static Rango de(Periodo periodo, LocalDate fecha) {
            return switch (periodo) {
                case DIA -> new Rango(fecha, fecha);
                case SEMANA -> {
                    LocalDate lunes = fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                    yield new Rango(lunes, lunes.plusDays(6));
                }
                case MES -> new Rango(fecha.withDayOfMonth(1), fecha.with(TemporalAdjusters.lastDayOfMonth()));
            };
        }

        Rango anterior(Periodo periodo) {
            return switch (periodo) {
                case DIA -> de(periodo, desde.minusDays(1));
                case SEMANA -> de(periodo, desde.minusWeeks(1));
                case MES -> de(periodo, desde.minusMonths(1));
            };
        }

        OffsetDateTime inicio() {
            return desde.atStartOfDay(ClockConfig.ZONA).toOffsetDateTime();
        }

        /** Exclusivo: el comienzo del día siguiente al último. */
        OffsetDateTime fin() {
            return hasta.plusDays(1).atStartOfDay(ClockConfig.ZONA).toOffsetDateTime();
        }
    }

    @Transactional(readOnly = true)
    public EstadisticasNegocioResponse calcular(UsuarioAutenticado usuario, Periodo periodo, LocalDate fecha) {
        Long negocioId = negocioActual.id(usuario);
        Rango actual = Rango.de(periodo, fecha != null ? fecha : LocalDate.now(clock.withZone(ClockConfig.ZONA)));
        Rango anterior = actual.anterior(periodo);

        List<OffsetDateTime> fechas = pedidos.fechasDePedidos(negocioId, actual.inicio(), actual.fin());
        long pedidosAnterior = pedidos.fechasDePedidos(negocioId, anterior.inicio(), anterior.fin()).size();

        long total = 0;
        long resueltas = 0;
        long abiertas = 0;
        for (IncidenciaRepository.IncidenciasPorEstado fila
                : incidencias.contarPorEstado(negocioId, actual.inicio(), actual.fin())) {
            total += fila.getTotal();
            if (fila.getEstado() == EstadoIncidencia.RESUELTA) {
                resueltas += fila.getTotal();
            } else {
                abiertas += fila.getTotal();
            }
        }

        return new EstadisticasNegocioResponse(
                periodo, actual.desde(), actual.hasta(),
                fechas.size(), pedidosAnterior,
                dinero(pedidos.facturacion(negocioId, actual.inicio(), actual.fin())),
                dinero(pedidos.facturacion(negocioId, anterior.inicio(), anterior.fin())),
                minutos(pedidos.minutosMediosEntrega(negocioId, actual.inicio(), actual.fin())),
                minutos(pedidos.minutosMediosEntrega(negocioId, anterior.inicio(), anterior.fin())),
                new Incidencias(total, resueltas, abiertas),
                serie(periodo, actual, fechas),
                pedidos.masVendidos(negocioId, actual.inicio(), actual.fin(), Limit.of(MAS_VENDIDOS)).stream()
                        .map(u -> new ProductoVendido(u.getNombre(), u.getUnidades()))
                        .toList());
    }

    /** Pedidos por hora (DIA), por día de la semana (SEMANA) o por día del mes (MES), con los ceros. */
    static List<Tramo> serie(Periodo periodo, Rango rango, List<OffsetDateTime> fechas) {
        int tramos = switch (periodo) {
            case DIA -> 24;
            case SEMANA -> 7;
            case MES -> rango.hasta().getDayOfMonth();
        };
        long[] cuenta = new long[tramos];
        for (OffsetDateTime f : fechas) {
            ZonedDateTime local = f.atZoneSameInstant(ClockConfig.ZONA);
            int tramo = switch (periodo) {
                case DIA -> local.getHour();
                case SEMANA -> local.getDayOfWeek().getValue() - 1;
                case MES -> local.getDayOfMonth() - 1;
            };
            cuenta[tramo]++;
        }
        List<Tramo> serie = new ArrayList<>(tramos);
        for (int i = 0; i < tramos; i++) {
            String etiqueta = switch (periodo) {
                case DIA -> "%02d:00".formatted(i);
                case SEMANA -> DIAS[i];
                case MES -> String.valueOf(i + 1);
            };
            serie.add(new Tramo(etiqueta, cuenta[i]));
        }
        return serie;
    }

    private static BigDecimal dinero(BigDecimal valor) {
        return (valor == null ? BigDecimal.ZERO : valor).setScale(2, RoundingMode.HALF_UP);
    }

    private static Integer minutos(Double valor) {
        return valor == null ? null : (int) Math.round(valor);
    }
}
