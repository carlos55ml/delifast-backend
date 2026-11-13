package dev.carlosmoran.delifast.dto;

import dev.carlosmoran.delifast.domain.Localidad;

public record LocalidadResponse(Long id, String nombre, String provincia) {

    public static LocalidadResponse de(Localidad localidad) {
        return new LocalidadResponse(localidad.getId(), localidad.getNombre(), localidad.getProvincia());
    }
}
