package dev.carlosmoran.delifast;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Comprueba que el contexto de Spring arranca y que Flyway conecta con la base de datos.
 * Necesita el fichero .env con las credenciales del proyecto Supabase delifast-dev.
 */
@SpringBootTest
@ActiveProfiles("dev")
class DeliFastApplicationTests {

    @Test
    void contextLoads() {
    }
}
