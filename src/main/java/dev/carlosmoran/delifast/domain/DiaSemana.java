package dev.carlosmoran.delifast.domain;

/** Día de la semana. Se guarda como número: 1 lunes ... 7 domingo. */
public enum DiaSemana {
    LUNES, MARTES, MIERCOLES, JUEVES, VIERNES, SABADO, DOMINGO;

    public short numero() {
        return (short) (ordinal() + 1);
    }

    public static DiaSemana deNumero(short numero) {
        return values()[numero - 1];
    }
}
