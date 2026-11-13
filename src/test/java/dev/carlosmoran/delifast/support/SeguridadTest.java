package dev.carlosmoran.delifast.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import dev.carlosmoran.delifast.config.CorsConfig;
import dev.carlosmoran.delifast.config.SecurityConfig;
import dev.carlosmoran.delifast.security.SupabaseJwtAuthenticationConverter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.UUID;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Utilidades para los tests de controladores (@WebMvcTest): cargan la configuración de
 * seguridad real y simulan el JWT de Supabase sin conectar con el JWKS.
 */
public final class SeguridadTest {

    public static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    public static final UUID GESTOR_ID = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    private SeguridadTest() {
    }

    /** Importa SecurityConfig y CorsConfig y sustituye el JwtDecoder (no se descarga el JWKS). */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Import({SecurityConfig.class, CorsConfig.class})
    @MockitoBean(types = JwtDecoder.class)
    public @interface ConSeguridad {
    }

    /** JWT con los claims que añade custom_access_token_hook, pasado por el conversor real. */
    public static JwtRequestPostProcessor token(UUID id, String rol, Long negocioId, boolean esDueno) {
        return jwt()
                .jwt(j -> {
                    j.subject(id.toString()).claim("user_rol", rol).claim("es_dueno", esDueno);
                    if (negocioId != null) {
                        j.claim("negocio_id", negocioId);
                    }
                })
                .authorities(SupabaseJwtAuthenticationConverter::autoridades);
    }

    public static JwtRequestPostProcessor admin() {
        return token(ADMIN_ID, "ADMINISTRADOR", null, false);
    }

    public static JwtRequestPostProcessor gestor(long negocioId) {
        return token(GESTOR_ID, "NEGOCIO", negocioId, true);
    }

    public static final UUID REPARTIDOR_ID = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    public static JwtRequestPostProcessor repartidor(long negocioId) {
        return token(REPARTIDOR_ID, "REPARTIDOR", negocioId, false);
    }

    public static JwtRequestPostProcessor cliente() {
        return token(UUID.fromString("00000000-0000-0000-0000-00000000000c"), "CLIENTE", null, false);
    }
}
