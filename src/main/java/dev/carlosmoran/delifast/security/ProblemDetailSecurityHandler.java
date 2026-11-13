package dev.carlosmoran.delifast.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Responde los 401 (sin token o token no válido) y 403 (rol sin permiso) de Spring Security
 * con application/problem+json, igual que GlobalExceptionHandler. Estos errores se producen
 * en los filtros, antes de llegar a los controladores, así que el @RestControllerAdvice no los ve.
 */
public class ProblemDetailSecurityHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        response.setHeader("WWW-Authenticate", "Bearer");
        escribir(request, response, HttpStatus.UNAUTHORIZED, "Falta el token de acceso o no es válido");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        escribir(request, response, HttpStatus.FORBIDDEN, "Tu rol no permite esta operación");
    }

    private static void escribir(HttpServletRequest request, HttpServletResponse response,
                                 HttpStatus status, String detalle) throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        // Valores fijos salvo la ruta, que se escapa: no hace falta un ObjectMapper.
        response.getWriter().write("""
                {"type":"about:blank","title":"%s","status":%d,"detail":"%s","instance":"%s"}"""
                .formatted(status.getReasonPhrase(), status.value(), detalle, json(request.getRequestURI())));
    }

    private static String json(String texto) {
        return texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
