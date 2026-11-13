package dev.carlosmoran.delifast.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI deliFastOpenApi() {
        return new OpenAPI().info(new Info()
                .title("DeliFast API")
                .description("API REST de DeliFast para la app móvil y la web")
                .version("0.0.1"));
    }
}
