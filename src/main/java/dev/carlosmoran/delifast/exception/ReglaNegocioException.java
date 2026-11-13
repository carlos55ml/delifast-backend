package dev.carlosmoran.delifast.exception;

/**
 * Se lanza cuando una operación incumple una regla de negocio
 * (pedido por debajo del mínimo, negocio cerrado, transición de estado no permitida...).
 */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
