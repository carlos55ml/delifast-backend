package dev.carlosmoran.delifast.dto.cliente;

import dev.carlosmoran.delifast.domain.Direccion;
import dev.carlosmoran.delifast.dto.LocalidadResponse;

public record DireccionResponse(Long id, String alias, String calle, String codigoPostal, LocalidadResponse localidad,
                                String indicaciones, boolean predeterminada) {

    public static DireccionResponse de(Direccion d) {
        return new DireccionResponse(d.getId(), d.getAlias(), d.getCalle(), d.getCodigoPostal(),
                LocalidadResponse.de(d.getLocalidad()), d.getIndicaciones(), d.isPredeterminada());
    }
}
