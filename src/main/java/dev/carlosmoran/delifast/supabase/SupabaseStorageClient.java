package dev.carlosmoran.delifast.supabase;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Supabase Storage por su API REST con la clave service_role. Los buckets logos y productos
 * son públicos (los crea V1): la URL pública sirve directamente en la web y la app.
 */
@Component
public class SupabaseStorageClient implements AlmacenImagenes {

    private final RestClient http;
    private final String prefijoPublico;
    private final boolean configurado;

    @Autowired
    public SupabaseStorageClient(@Value("${delifast.supabase.url}") String supabaseUrl,
                                 @Value("${delifast.supabase.service-role-key:}") String serviceRoleKey) {
        this(RestClient.builder(), supabaseUrl, serviceRoleKey);
    }

    SupabaseStorageClient(RestClient.Builder builder, String supabaseUrl, String serviceRoleKey) {
        this.configurado = serviceRoleKey != null && !serviceRoleKey.isBlank();
        this.prefijoPublico = supabaseUrl + "/storage/v1/object/public/";
        this.http = builder
                .baseUrl(supabaseUrl + "/storage/v1")
                .defaultHeader("apikey", serviceRoleKey)
                .defaultHeader("Authorization", "Bearer " + serviceRoleKey)
                .build();
    }

    @Override
    public String subir(String bucket, String ruta, byte[] contenido, String contentType) {
        comprobarConfigurado();
        try {
            http.post().uri(rutaObjeto(bucket, ruta))
                    .contentType(MediaType.parseMediaType(contentType))
                    .header("cache-control", "max-age=31536000")
                    .body(contenido)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new ServicioExternoException("No se pudo subir la imagen a Supabase Storage", e);
        }
        return prefijoPublico + bucket + "/" + ruta;
    }

    @Override
    public void borrar(String urlPublica) {
        if (urlPublica == null || !urlPublica.startsWith(prefijoPublico)) {
            return;
        }
        comprobarConfigurado();
        String bucketYRuta = urlPublica.substring(prefijoPublico.length());
        int barra = bucketYRuta.indexOf('/');
        if (barra < 0) {
            return;
        }
        try {
            http.delete().uri(rutaObjeto(bucketYRuta.substring(0, barra), bucketYRuta.substring(barra + 1)))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new ServicioExternoException("No se pudo borrar la imagen de Supabase Storage", e);
        }
    }

    /**
     * La ruta lleva "/" (negocio/fichero), que una plantilla {ruta} codificaría como %2F. Las
     * rutas las genera el backend (id del negocio y UUID), así que se pueden concatenar.
     */
    private static String rutaObjeto(String bucket, String ruta) {
        if (!ruta.matches("[A-Za-z0-9._/-]+") || ruta.contains("..")) {
            throw new IllegalArgumentException("Ruta de Storage no válida: " + ruta);
        }
        return "/object/" + bucket + "/" + ruta;
    }

    private void comprobarConfigurado() {
        if (!configurado) {
            throw new ServicioExternoException("Falta SUPABASE_SERVICE_ROLE_KEY: no se pueden gestionar imágenes");
        }
    }
}
