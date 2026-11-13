package dev.carlosmoran.delifast.dto.negocio;

import dev.carlosmoran.delifast.dto.LocalidadResponse;
import java.math.BigDecimal;
import java.util.List;

/** Cabecera del panel y configuración del negocio (GET /negocio, N07). */
public record MiNegocioResponse(
        Long id,
        String nombre,
        String descripcion,
        LocalidadResponse localidad,
        String direccion,
        String telefono,
        String logoUrl,
        BigDecimal gastosEnvio,
        BigDecimal pedidoMinimo,
        List<FranjaHorarioDto> horarios,
        boolean abierto,
        boolean abiertoManual,
        long pedidosHoy) {
}
