package dev.carlosmoran.delifast.supabase;

/**
 * Almacén de imágenes públicas (Supabase Storage). Interfaz para sustituirla en los tests.
 */
public interface AlmacenImagenes {

    /**
     * Sube el fichero y devuelve su URL pública.
     *
     * @throws ServicioExternoException si Storage no responde o devuelve un error
     */
    String subir(String bucket, String ruta, byte[] contenido, String contentType);

    /** Borra el fichero de una URL pública devuelta por {@link #subir}. Las URL ajenas se ignoran. */
    void borrar(String urlPublica);
}
