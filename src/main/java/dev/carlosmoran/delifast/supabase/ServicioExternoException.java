package dev.carlosmoran.delifast.supabase;

/** Fallo al llamar a un servicio externo (Supabase). Se responde 502. */
public class ServicioExternoException extends RuntimeException {

    public ServicioExternoException(String mensaje) {
        super(mensaje);
    }

    public ServicioExternoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
