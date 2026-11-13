package dev.carlosmoran.delifast.supabase;

import dev.carlosmoran.delifast.exception.ConflictoException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Implementación con la API REST de Supabase Auth (GoTrue), autenticada con la clave service_role.
 *
 * El alta va en dos pasos porque /invite guarda los datos extra en user_metadata y no deja
 * fijar app_metadata: primero POST /admin/users con app_metadata (rol, negocio_id, es_dueno)
 * y sin confirmar el email, y después POST /invite, que envía la invitación a esa cuenta.
 */
@Component
public class SupabaseAuthAdminClient implements SupabaseAuthAdmin {

    /** Códigos de error de Supabase Auth cuando el email ya existe. */
    private static final Set<String> EMAIL_REPETIDO = Set.of("email_exists", "user_already_exists");
    /** "Para siempre" en formato de duración de Go (100 años). */
    private static final String BLOQUEO_INDEFINIDO = "876000h";
    private static final ParameterizedTypeReference<Map<String, Object>> MAPA = new ParameterizedTypeReference<>() {
    };

    private final RestClient http;
    private final boolean configurado;
    /** Página de la web a la que lleva el enlace del email (crear contraseña). */
    private final String urlInvitacion;

    @Autowired
    public SupabaseAuthAdminClient(@Value("${delifast.supabase.url}") String supabaseUrl,
                                   @Value("${delifast.supabase.service-role-key:}") String serviceRoleKey,
                                   @Value("${delifast.web.url-invitacion}") String urlInvitacion) {
        this(RestClient.builder(), supabaseUrl, serviceRoleKey, urlInvitacion);
    }

    /** Recibe el builder para poder enlazarlo a MockRestServiceServer en los tests. */
    SupabaseAuthAdminClient(RestClient.Builder builder, String supabaseUrl, String serviceRoleKey,
                            String urlInvitacion) {
        this.urlInvitacion = urlInvitacion;
        this.configurado = serviceRoleKey != null && !serviceRoleKey.isBlank();
        this.http = builder
                .baseUrl(supabaseUrl + "/auth/v1")
                .defaultHeader("apikey", serviceRoleKey)
                .defaultHeader("Authorization", "Bearer " + serviceRoleKey)
                .build();
    }

    @Override
    public UUID invitar(NuevaCuenta cuenta) {
        Map<String, Object> appMetadata = new HashMap<>();
        appMetadata.put("rol", cuenta.rol().name());
        appMetadata.put("negocio_id", cuenta.negocioId());
        appMetadata.put("es_dueno", cuenta.esDueno());

        // Lo lee el trigger crear_perfil al insertar el usuario (perfiles.nombre y telefono).
        Map<String, Object> userMetadata = new HashMap<>();
        userMetadata.put("nombre", cuenta.nombre());
        if (cuenta.telefono() != null) {
            userMetadata.put("telefono", cuenta.telefono());
        }

        Map<String, Object> usuario = llamar(() -> http.post().uri("/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "email", cuenta.email(),
                        "email_confirm", false,
                        "user_metadata", userMetadata,
                        "app_metadata", appMetadata))
                .retrieve()
                .body(MAPA));
        UUID id = UUID.fromString(String.valueOf(usuario.get("id")));

        try {
            // Como inviteUserByEmail de supabase-js: redirect_to va en la query. La URL tiene que
            // estar en Authentication → URL Configuration → Redirect URLs del proyecto Supabase.
            llamar(() -> http.post().uri(u -> u.path("/invite").queryParam("redirect_to", "{url}").build(urlInvitacion))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("email", cuenta.email()))
                    .retrieve()
                    .toBodilessEntity());
        } catch (RuntimeException e) {
            eliminarSinFallar(id);
            throw e;
        }
        return id;
    }

    @Override
    public void eliminar(UUID id) {
        llamar(() -> http.delete().uri("/admin/users/{id}", id).retrieve().toBodilessEntity());
    }

    @Override
    public void bloquear(UUID id, boolean bloqueada) {
        llamar(() -> http.put().uri("/admin/users/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("ban_duration", bloqueada ? BLOQUEO_INDEFINIDO : "none"))
                .retrieve()
                .toBodilessEntity());
    }

    private void eliminarSinFallar(UUID id) {
        try {
            eliminar(id);
        } catch (RuntimeException ignorada) {
            // Queda una cuenta sin invitación; el administrador puede reintentar el alta con otro email
            // o borrarla desde el panel de Supabase.
        }
    }

    private <T> T llamar(Supplier<T> peticion) {
        if (!configurado) {
            throw new ServicioExternoException("Falta SUPABASE_SERVICE_ROLE_KEY: no se pueden gestionar cuentas");
        }
        try {
            return peticion.get();
        } catch (RestClientResponseException e) {
            if (esEmailRepetido(e.getStatusCode(), e.getResponseBodyAsString())) {
                throw new ConflictoException("Ya existe una cuenta con ese email");
            }
            throw new ServicioExternoException("Supabase Auth respondió " + e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            throw new ServicioExternoException("No se pudo contactar con Supabase Auth", e);
        }
    }

    static boolean esEmailRepetido(HttpStatusCode status, String cuerpo) {
        return (status.value() == 422 || status.value() == 409 || status.value() == 400)
                && cuerpo != null
                && EMAIL_REPETIDO.stream().anyMatch(cuerpo::contains);
    }
}
