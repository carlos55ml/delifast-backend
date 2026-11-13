package dev.carlosmoran.delifast.security;

import java.util.Collection;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Convierte el JWT de Supabase en una autenticación con la autoridad ROLE_&lt;rol&gt;
 * (ROLE_CLIENTE, ROLE_NEGOCIO, ROLE_REPARTIDOR o ROLE_ADMINISTRADOR).
 */
public class SupabaseJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        return new JwtAuthenticationToken(jwt, autoridades(jwt), jwt.getSubject());
    }

    public static Collection<GrantedAuthority> autoridades(Jwt jwt) {
        return List.of(new SimpleGrantedAuthority("ROLE_" + UsuarioAutenticado.desde(jwt).rol().name()));
    }
}
