package dev.carlosmoran.delifast.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import java.util.Set;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentación OpenAPI que genera springdoc a partir de los controladores
 * (/v3/api-docs y Swagger UI en /swagger-ui.html). Cada operación lleva el operationId,
 * el resumen y la etiqueta del contrato docs/api/openapi.yaml, que sigue siendo la fuente
 * de verdad; OpenApiGeneradoTest comprueba que los dos tienen las mismas rutas.
 */
@Configuration
public class OpenApiConfig {

    public static final String VERSION = "0.8.0";
    private static final String BEARER = "bearerAuth";

    /** Rutas públicas: sin candado en Swagger UI (SecurityConfig las deja pasar sin token). */
    private static final Set<String> PUBLICAS = Set.of("/localidades", "/negocios", "/negocios/{id}/carta");

    /** Orden de las etiquetas en Swagger UI, por zona de la plataforma. */
    private static final List<String> ETIQUETAS = List.of(
            "Perfil", "Localidades", "Administración",
            "Negocio · Pedidos", "Negocio · Carta", "Negocio · Configuración", "Negocio · Equipo",
            "Negocio · Incidencias", "Negocio · Estadísticas",
            "Repartidor",
            "Cliente · Negocios", "Cliente · Direcciones", "Cliente · Pedidos");

    @Bean
    OpenAPI deliFastOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("DeliFast API")
                        .version(VERSION)
                        .description("""
                                API REST de DeliFast para la web (administración, negocio y repartidor) y la \
                                app del cliente. Documentación generada por springdoc desde el código; el \
                                contrato acordado con la web y la app está en docs/api/openapi.yaml.

                                Autenticación: botón **Authorize** con el access token de Supabase Auth \
                                (`Authorization: Bearer <JWT>`). El rol sale del claim `user_rol`.

                                Errores en `application/problem+json` (RFC 9457): 400 validación (con \
                                `errores` = ["campo: mensaje"]), 401 sin token, 403 rol o cuenta sin permiso, \
                                404 no existe (o es de otro negocio/cliente), 409 conflicto, 422 regla de \
                                negocio, 502 fallo de Supabase."""))
                .servers(List.of(new Server().url("http://localhost:8080").description("Desarrollo local")))
                .tags(ETIQUETAS.stream().map(nombre -> new Tag().name(nombre)).toList())
                .components(new Components()
                        .addSecuritySchemes(BEARER, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Access token de Supabase Auth.")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }

    /**
     * Quita el candado de las rutas públicas y añade a las protegidas las respuestas 401 y 403
     * en formato Problema (las escriben los filtros de seguridad, que springdoc no ve).
     */
    @Bean
    OpenApiCustomizer seguridadPorOperacion() {
        return openApi -> {
            // springdoc rehace components.schemas con los DTO; el esquema Problema se añade aquí.
            openApi.getComponents().addSchemas("Problema", problema());
            openApi.getPaths().forEach((ruta, item) -> item.readOperations().forEach(op -> {
            if (PUBLICAS.contains(ruta)) {
                op.setSecurity(List.of());
                return;
            }
            op.getResponses().addApiResponse("401", problema("Falta el token o no es válido"));
            op.getResponses().addApiResponse("403", problema("El rol o la cuenta no permiten la operación"));
            }));
        };
    }

    private static ApiResponse problema(String descripcion) {
        return new ApiResponse().description(descripcion).content(new Content().addMediaType(
                org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                new MediaType().schema(new Schema<>().$ref("#/components/schemas/Problema"))));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Schema<?> problema() {
        Schema errores = new Schema<>().type("array").items(new Schema<>().type("string"))
                .description("Solo en 400. Cada elemento es \"campo: mensaje\".");
        return new Schema<>()
                .type("object")
                .description("ProblemDetail (RFC 9457) de Spring.")
                .addProperty("type", new Schema<>().type("string"))
                .addProperty("title", new Schema<>().type("string"))
                .addProperty("status", new Schema<>().type("integer"))
                .addProperty("detail", new Schema<>().type("string"))
                .addProperty("instance", new Schema<>().type("string"))
                .addProperty("errores", errores)
                .required(List.of("status"));
    }
}
