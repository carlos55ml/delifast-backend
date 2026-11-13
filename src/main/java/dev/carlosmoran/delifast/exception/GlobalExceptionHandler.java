package dev.carlosmoran.delifast.exception;

import dev.carlosmoran.delifast.supabase.ServicioExternoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.FieldError;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
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

    /** 403 lanzado desde un servicio (solo el dueño, negocio suspendido...). */
    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail prohibido(AccessDeniedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
                ex.getMessage() != null ? ex.getMessage() : "Tu rol no permite esta operación");
    }

    /** Parámetro con un valor que no encaja (por ejemplo ?estado=NOEXISTE o un id no numérico). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail tipoIncorrecto(MethodArgumentTypeMismatchException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Datos de entrada no válidos");
        problem.setProperty("errores", List.of(ex.getName() + ": valor no válido"));
        return problem;
    }

    /**
     * Validación de parámetros que no son un objeto (por ejemplo el array de PUT /negocio/horarios).
     * Cada error sale como "campo[i].propiedad: mensaje".
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    ProblemDetail validacionMetodo(HandlerMethodValidationException ex) {
        List<String> errores = new ArrayList<>();
        ex.getParameterValidationResults().forEach(resultado -> resultado.getResolvableErrors().forEach(error -> {
            StringBuilder nombre = new StringBuilder(resultado.getMethodParameter().getParameterName());
            if (resultado.getContainerIndex() != null) {
                nombre.append('[').append(resultado.getContainerIndex()).append(']');
            }
            if (error instanceof FieldError fe) {
                nombre.append('.').append(fe.getField());
            }
            errores.add(nombre + ": " + error.getDefaultMessage());
        }));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Datos de entrada no válidos");
        problem.setProperty("errores", errores);
        return problem;
    }

    @ExceptionHandler(ValidacionException.class)
    ProblemDetail validacionServicio(ValidacionException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setProperty("errores", ex.getErrores());
        return problem;
    }

    /** Fichero por encima de spring.servlet.multipart.max-file-size (2 MB). */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ProblemDetail ficheroGrande(MaxUploadSizeExceededException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Datos de entrada no válidos");
        problem.setProperty("errores", List.of("imagen: no puede superar 2 MB"));
        return problem;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ProblemDetail faltaParametro(MissingServletRequestParameterException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Datos de entrada no válidos");
        problem.setProperty("errores", List.of(ex.getParameterName() + ": es obligatorio"));
        return problem;
    }

    /** Falta la parte "datos" de un multipart. */
    @ExceptionHandler(MissingServletRequestPartException.class)
    ProblemDetail faltaParte(MissingServletRequestPartException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Datos de entrada no válidos");
        problem.setProperty("errores", List.of(ex.getRequestPartName() + ": es obligatorio"));
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
