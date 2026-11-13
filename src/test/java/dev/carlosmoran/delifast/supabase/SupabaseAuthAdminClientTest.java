package dev.carlosmoran.delifast.supabase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import dev.carlosmoran.delifast.domain.Rol;
import dev.carlosmoran.delifast.exception.ConflictoException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SupabaseAuthAdminClientTest {

    private static final String URL = "https://proyecto.supabase.co";
    private static final String INVITACION = "http://localhost:3000/auth/confirmar?siguiente=/nueva-contrasena";
    private static final UUID ID = UUID.fromString("5b0c1d4e-0000-4000-8000-000000000001");
    private static final NuevaCuenta LOLA = new NuevaCuenta("lola@lasirena.es", "Lola", Rol.NEGOCIO, 7L, true);

    private MockRestServiceServer servidor;
    private SupabaseAuthAdminClient cliente;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        cliente = new SupabaseAuthAdminClient(builder, URL, "clave-service-role", INVITACION);
    }

    @Test
    void creaLaCuentaConAppMetadataYLuegoInvita() {
        servidor.expect(requestTo(URL + "/auth/v1/admin/users"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer clave-service-role"))
                .andExpect(header("apikey", "clave-service-role"))
                .andExpect(jsonPath("$.email").value("lola@lasirena.es"))
                .andExpect(jsonPath("$.email_confirm").value(false))
                .andExpect(jsonPath("$.app_metadata.rol").value("NEGOCIO"))
                .andExpect(jsonPath("$.app_metadata.negocio_id").value(7))
                .andExpect(jsonPath("$.app_metadata.es_dueno").value(true))
                .andExpect(jsonPath("$.user_metadata.nombre").value("Lola"))
                .andRespond(withSuccess("{\"id\":\"" + ID + "\",\"email\":\"lola@lasirena.es\"}", MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(org.hamcrest.Matchers.startsWith(URL + "/auth/v1/invite?redirect_to=")))
                // Va codificada en la query; Supabase la decodifica.
                .andExpect(queryParam("redirect_to", java.net.URLEncoder.encode(INVITACION, java.nio.charset.StandardCharsets.UTF_8)))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.email").value("lola@lasirena.es"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThat(cliente.invitar(LOLA)).isEqualTo(ID);
        servidor.verify();
    }

    @Test
    void emailRepetidoEsConflicto() {
        servidor.expect(requestTo(URL + "/auth/v1/admin/users"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_CONTENT).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":422,\"error_code\":\"email_exists\",\"msg\":\"A user with this email address has already been registered\"}"));

        assertThatThrownBy(() -> cliente.invitar(LOLA)).isInstanceOf(ConflictoException.class);
    }

    @Test
    void siFallaLaInvitacionBorraLaCuenta() {
        servidor.expect(requestTo(URL + "/auth/v1/admin/users"))
                .andRespond(withSuccess("{\"id\":\"" + ID + "\"}", MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(org.hamcrest.Matchers.startsWith(URL + "/auth/v1/invite"))).andRespond(withServerError());
        servidor.expect(requestTo(URL + "/auth/v1/admin/users/" + ID))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withSuccess());

        assertThatThrownBy(() -> cliente.invitar(LOLA)).isInstanceOf(ServicioExternoException.class);
        servidor.verify();
    }

    @Test
    void bloquearYDesbloquear() {
        servidor.expect(requestTo(URL + "/auth/v1/admin/users/" + ID))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(jsonPath("$.ban_duration").value("876000h"))
                .andRespond(withSuccess());
        servidor.expect(requestTo(URL + "/auth/v1/admin/users/" + ID))
                .andExpect(jsonPath("$.ban_duration").value("none"))
                .andRespond(withSuccess());

        cliente.bloquear(ID, true);
        cliente.bloquear(ID, false);
        servidor.verify();
    }

    @Test
    void sinClaveServiceRoleNoLlama() {
        SupabaseAuthAdminClient sinClave = new SupabaseAuthAdminClient(RestClient.builder(), URL, "", INVITACION);

        assertThatThrownBy(() -> sinClave.invitar(LOLA))
                .isInstanceOf(ServicioExternoException.class)
                .hasMessageContaining("SUPABASE_SERVICE_ROLE_KEY");
    }
}
