package dev.carlosmoran.delifast;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Comprueba que el contexto de Spring arranca y que Flyway conecta con la base de datos.
 * Necesita el fichero .env con las credenciales del proyecto Supabase delifast-dev y red que
 * llegue al pooler, así que no se ejecuta por defecto: ./mvnw test -Dgrupos.excluidos=none
 */
@Tag("bd")
@SpringBootTest
@ActiveProfiles("dev")
class DeliFastApplicationTests {

    @Test
    void contextLoads() {
    }
}
