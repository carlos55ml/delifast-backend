package dev.carlosmoran.delifast.security;

import static org.assertj.core.api.Assertions.assertThat;

import dev.carlosmoran.delifast.domain.Rol;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class SupabaseJwtAuthenticationConverterTest {

    private final SupabaseJwtAuthenticationConverter converter = new SupabaseJwtAuthenticationConverter();

    private static Jwt.Builder token() {
        return Jwt.withTokenValue("t").header("alg", "ES256")
                .subject("11111111-1111-1111-1111-111111111111")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(3600));
    }

    @Test
    void usaElClaimUserRolDelHook() {
        Jwt jwt = token().claim("user_rol", "ADMINISTRADOR").build();

        assertThat(converter.convert(jwt).getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMINISTRADOR");
    }

    @Test
    void sinHookLeeElRolDeAppMetadata() {
        Jwt jwt = token().claim("app_metadata", Map.of("rol", "NEGOCIO", "negocio_id", 7, "es_dueno", true)).build();

        UsuarioAutenticado usuario = UsuarioAutenticado.desde(jwt);

        assertThat(usuario.rol()).isEqualTo(Rol.NEGOCIO);
        assertThat(usuario.negocioId()).isEqualTo(7L);
        assertThat(usuario.esDueno()).isTrue();
        assertThat(converter.convert(jwt).getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_NEGOCIO");
    }

    @Test
    void sinRolEsCliente() {
        Jwt jwt = token().build();

        assertThat(UsuarioAutenticado.desde(jwt).rol()).isEqualTo(Rol.CLIENTE);
        assertThat(converter.convert(jwt).getName()).isEqualTo("11111111-1111-1111-1111-111111111111");
    }

    @Test
    void rolDesconocidoEsCliente() {
        Jwt jwt = token().claim("user_rol", "SUPERUSUARIO").build();

        assertThat(UsuarioAutenticado.desde(jwt).rol()).isEqualTo(Rol.CLIENTE);
    }
}
