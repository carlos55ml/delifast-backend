package dev.carlosmoran.delifast.security;

import dev.carlosmoran.delifast.domain.Rol;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Datos del usuario que salen del JWT de Supabase: el id (claim sub) y los claims que
 * añade custom_access_token_hook (user_rol, negocio_id, es_dueno). Si el hook aún no está
 * activado, se leen de app_metadata, que solo puede escribir el backend.
 */
public record UsuarioAutenticado(UUID id, Rol rol, Long negocioId, boolean esDueno) {

    public static UsuarioAutenticado desde(Jwt jwt) {
        Map<String, Object> appMetadata = jwt.hasClaim("app_metadata")
                ? jwt.getClaimAsMap("app_metadata")
                : Map.of();
        return new UsuarioAutenticado(
                UUID.fromString(jwt.getSubject()),
                rol(jwt, appMetadata),
                negocioId(jwt.getClaim("negocio_id") != null ? jwt.getClaim("negocio_id") : appMetadata.get("negocio_id")),
                Boolean.parseBoolean(String.valueOf(
                        jwt.getClaim("es_dueno") != null ? jwt.getClaim("es_dueno") : appMetadata.get("es_dueno"))));
    }

    /** Rol del token. Sin claim (cuenta recién registrada desde la app) es CLIENTE. */
    static Rol rol(Jwt jwt, Map<String, Object> appMetadata) {
        Object valor = jwt.getClaim("user_rol");
        if (valor == null) {
            valor = appMetadata.get("rol");
        }
        if (valor == null) {
            return Rol.CLIENTE;
        }
        try {
            return Rol.valueOf(valor.toString());
        } catch (IllegalArgumentException e) {
            return Rol.CLIENTE;
        }
    }

    private static Long negocioId(Object valor) {
        if (valor instanceof Number n) {
            return n.longValue();
        }
        if (valor instanceof String s && !s.isBlank()) {
            return Long.valueOf(s);
        }
        return null;
    }
}
