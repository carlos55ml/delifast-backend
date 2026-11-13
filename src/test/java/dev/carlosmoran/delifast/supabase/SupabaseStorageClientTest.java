package dev.carlosmoran.delifast.supabase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SupabaseStorageClientTest {

    private static final String URL = "https://proyecto.supabase.co";

    @Test
    void subeYDevuelveLaUrlPublica() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        SupabaseStorageClient cliente = new SupabaseStorageClient(builder, URL, "clave");
        servidor.expect(requestTo(URL + "/storage/v1/object/productos/7/a.png"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer clave"))
                .andExpect(header("Content-Type", "image/png"))
                .andRespond(withSuccess());

        String url = cliente.subir("productos", "7/a.png", new byte[] {1}, "image/png");

        assertThat(url).isEqualTo(URL + "/storage/v1/object/public/productos/7/a.png");
        servidor.verify();
    }

    @Test
    void borraAPartirDeLaUrlPublicaEIgnoraLasAjenas() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        SupabaseStorageClient cliente = new SupabaseStorageClient(builder, URL, "clave");
        servidor.expect(requestTo(URL + "/storage/v1/object/logos/7/a.png"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withSuccess());

        cliente.borrar(URL + "/storage/v1/object/public/logos/7/a.png");
        cliente.borrar("https://otro.sitio/logo.png");
        cliente.borrar(null);
        servidor.verify();
    }
}
