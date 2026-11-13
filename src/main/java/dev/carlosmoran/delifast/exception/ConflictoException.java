package dev.carlosmoran.delifast.exception;

/**
 * Se lanza cuando la operación choca con datos que ya existen (por ejemplo, un email ya registrado).
 */
public class ConflictoException extends RuntimeException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
