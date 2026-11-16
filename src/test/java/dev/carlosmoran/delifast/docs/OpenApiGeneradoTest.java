package dev.carlosmoran.delifast.docs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.carlosmoran.delifast.config.OpenApiConfig;
import dev.carlosmoran.delifast.support.SinBaseDeDatos;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.yaml.snakeyaml.Yaml;

/**
 * Genera la documentación OpenAPI de springdoc sin base de datos y la guarda en
 * docs/api/generado/ (JSON, YAML y una página de Swagger UI que se abre sin servidor).
 * Además comprueba que el código implementa exactamente las rutas del contrato
 * docs/api/openapi.yaml, con los mismos operationId y la misma versión.
 */
@SpringBootTest(properties = SinBaseDeDatos.SIN_BD)
@AutoConfigureMockMvc
@Import(SinBaseDeDatos.class)
class OpenApiGeneradoTest {

    private static final Path CONTRATO = Path.of("docs/api/openapi.yaml");
    private static final Path SALIDA = Path.of("docs/api/generado");
    private static final Set<String> METODOS = Set.of("get", "post", "put", "patch", "delete");

    @Autowired
    MockMvc mvc;

    @Test
    void generaLaDocumentacionYCoincideConElContrato() throws Exception {
        String json = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String yaml = mvc.perform(get("/v3/api-docs.yaml")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        Files.createDirectories(SALIDA);
        Files.writeString(SALIDA.resolve("openapi.json"), json);
        Files.writeString(SALIDA.resolve("openapi.yaml"), yaml);
        Files.writeString(SALIDA.resolve("swagger.html"), paginaSwagger(json));

        Map<String, Object> generado = new Yaml().load(json);
        Map<String, Object> contrato = cargar(CONTRATO);
        assertThat(operaciones(generado)).as("rutas y operationId del código frente al contrato")
                .isEqualTo(operaciones(contrato));
        assertThat(((Map<?, ?>) contrato.get("info")).get("version")).as("versión del contrato en OpenApiConfig")
                .isEqualTo(OpenApiConfig.VERSION);
    }

    @Test
    void swaggerUiYElContratoSeSirvenSinToken() throws Exception {
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(get("/contrato/openapi.yaml")).andExpect(status().isOk());
    }

    /** "GET /negocios/{id}/carta obtenerCartaCliente" por cada operación. */
    @SuppressWarnings("unchecked")
    private static Set<String> operaciones(Map<String, Object> api) {
        Set<String> resultado = new TreeSet<>();
        ((Map<String, Map<String, Object>>) api.get("paths")).forEach((ruta, item) -> item.forEach((metodo, op) -> {
            if (METODOS.contains(metodo)) {
                resultado.add(metodo.toUpperCase() + " " + ruta + " " + ((Map<String, Object>) op).get("operationId"));
            }
        }));
        return resultado;
    }

    private static Map<String, Object> cargar(Path fichero) throws IOException {
        return new Yaml().load(Files.readString(fichero));
    }

    /** Swagger UI desde CDN con la especificación incrustada: se abre con doble clic, sin servidor. */
    private static String paginaSwagger(String json) {
        return """
                <!doctype html>
                <html lang="es">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>DeliFast API</title>
                  <link rel="stylesheet" href="https://unpkg.com/swagger-ui-dist@5.17.14/swagger-ui.css">
                </head>
                <body>
                  <!-- Generado por OpenApiGeneradoTest a partir de /v3/api-docs. No editar a mano. -->
                  <div id="swagger-ui"></div>
                  <script src="https://unpkg.com/swagger-ui-dist@5.17.14/swagger-ui-bundle.js"></script>
                  <script>
                    const spec = %s;
                    SwaggerUIBundle({ spec, dom_id: "#swagger-ui", deepLinking: true, persistAuthorization: true,
                                      tagsSorter: "alpha", operationsSorter: "method" });
                  </script>
                </body>
                </html>
                """.formatted(json.replace("</", "<\\/"));
    }
}
