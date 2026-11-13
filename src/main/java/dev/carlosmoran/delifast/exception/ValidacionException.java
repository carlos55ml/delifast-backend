package dev.carlosmoran.delifast.exception;

import java.util.List;

/**
 * Datos de entrada no válidos que solo se pueden comprobar en el servicio (imagen con un tipo
 * no permitido, franjas de horario solapadas...). Se responde 400 con la lista "campo: mensaje",
 * igual que los errores de Bean Validation.
 */
public class ValidacionException extends RuntimeException {

    private final List<String> errores;

    public ValidacionException(List<String> errores) {
        super("Datos de entrada no válidos");
        this.errores = List.copyOf(errores);
    }

    public ValidacionException(String campo, String mensaje) {
        this(List.of(campo + ": " + mensaje));
    }

    public List<String> getErrores() {
        return errores;
    }
}
