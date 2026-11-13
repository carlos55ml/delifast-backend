package dev.carlosmoran.delifast.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Convierte DiaSemana a la columna smallint horarios_apertura.dia (1..7). */
@Converter
public class DiaSemanaConverter implements AttributeConverter<DiaSemana, Short> {

    @Override
    public Short convertToDatabaseColumn(DiaSemana dia) {
        return dia == null ? null : dia.numero();
    }

    @Override
    public DiaSemana convertToEntityAttribute(Short numero) {
        return numero == null ? null : DiaSemana.deNumero(numero);
    }
}
