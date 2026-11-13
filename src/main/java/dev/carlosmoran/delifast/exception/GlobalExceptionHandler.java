package dev.carlosmoran.delifast.exception;

import dev.carlosmoran.delifast.supabase.ServicioExternoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import java.util.List;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones a respuestas JSON con formato ProblemDetail (RFC 9457).
 * Los 401 y 403 de los filtros de seguridad los escribe ProblemDetailSecurityHandler.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ReglaNegocioException.class)
    ProblemDetail reglaNegocio(ReglaNegocioException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    ProblemDetail noEncontrado(RecursoNoEncontradoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflictoException.class)
    ProblemDetail conflicto(ConflictoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ServicioExternoException.class)
    ProblemDetail servicioExterno(ServicioExternoException ex) {
        log.error("Fallo en un servicio externo", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY,
                "No se ha podido completar la operación con el servicio de cuentas. Inténtalo de nuevo.");
    }

    /** Por si un 403 llega desde un controlador o servicio (seguridad por método). */
    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail prohibido(AccessDeniedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Tu rol no permite esta operación");
    }

    /** Parámetro con un valor que no encaja (por ejemplo ?estado=NOEXISTE o un id no numérico). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail tipoIncorrecto(MethodArgumentTypeMismatchException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Datos de entrada no válidos");
        problem.setProperty("errores", List.of(ex.getName() + ": valor no válido"));
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail cuerpoIlegible(HttpMessageNotReadableException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es un JSON válido");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validacion(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Datos de entrada no válidos");
        problem.setProperty("errores", ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList());
        return problem;
    }
}
