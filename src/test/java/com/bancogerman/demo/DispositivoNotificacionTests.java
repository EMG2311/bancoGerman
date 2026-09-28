package com.bancogerman.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import com.bancogerman.demo.notificacion.push.ResultadoPush;
import org.junit.jupiter.api.Test;

class DispositivoNotificacionTests extends ApiTestBase {

	@Test
	void registrarElMismoTokenNoDuplicaYSeReasignaAlUltimoUsuario() throws Exception {
		Usr ana = registrar("Ana");
		Usr beto = registrar("Beto");
		String token = "token-" + UUID.randomUUID();
		String body = """
				{"pushToken":"%s","plataforma":"ANDROID"}""".formatted(token);

		Long id = idDe(post(ana, "/api/dispositivos", body).andExpect(status().isOk()));
		assertThat(idDe(post(ana, "/api/dispositivos", body))).isEqualTo(id);
		assertThat((List<?>) leer(get(ana, "/api/dispositivos"), "$")).hasSize(1);

		// Beto inicia sesión en el mismo teléfono
		assertThat(idDe(post(beto, "/api/dispositivos", body))).isEqualTo(id);
		assertThat((List<?>) leer(get(ana, "/api/dispositivos"), "$")).isEmpty();
		assertThat((List<?>) leer(get(beto, "/api/dispositivos"), "$")).hasSize(1);

		delete(ana, "/api/dispositivos/" + id).andExpect(status().isNotFound());
		delete(beto, "/api/dispositivos/" + id).andExpect(status().isNoContent());
	}

	@Test
	void unTokenInvalidoSeBorraPeroLaNotificacionQuedaEnElHistorial() throws Exception {
		Usr banco = registrar("Banco");
		Usr deudor = registrar("Deudor");
		post(deudor, "/api/dispositivos", """
				{"pushToken":"token-viejo-%s","plataforma":"ANDROID"}""".formatted(deudor.id()));
		when(pushSender.enviar(any())).thenReturn(ResultadoPush.TOKEN_INVALIDO);

		invitar(banco, deudor, false);

		esperarHasta(() -> ((List<?>) leer(get(deudor, "/api/dispositivos"), "$")).isEmpty());
		get(deudor, "/api/notificaciones").andExpect(jsonPath("$.totalElementos").value(1));
	}

	@Test
	void unPushQueExplotaNoRompeLaOperacion() throws Exception {
		Usr banco = registrar("Banco");
		Usr deudor = registrar("Deudor");
		post(deudor, "/api/dispositivos", """
				{"pushToken":"token-%s","plataforma":"WEB"}""".formatted(deudor.id()));
		when(pushSender.enviar(any())).thenThrow(new RuntimeException("FCM caído"));

		Long id = invitar(banco, deudor, false);

		verify(pushSender, timeout(5000)).enviar(any());
		get(banco, "/api/relaciones/" + id).andExpect(status().isOk());
		assertThat((List<?>) leer(get(deudor, "/api/dispositivos"), "$")).hasSize(1);
		get(deudor, "/api/notificaciones").andExpect(jsonPath("$.totalElementos").value(1));
	}

	@Test
	void marcarNotificacionesComoLeidas() throws Exception {
		Usr banco = registrar("Banco");
		Usr deudor = registrar("Deudor");
		Usr otro = registrar("Otro");
		Long relacion = relacionActiva(banco, deudor, false);
		cargo(banco, relacion, "100");
		cargo(banco, relacion, "200");

		get(deudor, "/api/notificaciones/no-leidas/cantidad").andExpect(jsonPath("$.cantidad").value(3));
		Long ultima = ((Number) leer(get(deudor, "/api/notificaciones"), "$.contenido[0].id")).longValue();

		patch(otro, "/api/notificaciones/" + ultima + "/leida", "").andExpect(status().isNotFound());
		patch(deudor, "/api/notificaciones/" + ultima + "/leida", "").andExpect(status().isOk())
				.andExpect(jsonPath("$.leida").value(true));
		get(deudor, "/api/notificaciones?soloNoLeidas=true").andExpect(jsonPath("$.totalElementos").value(2));

		post(deudor, "/api/notificaciones/leer-todas").andExpect(jsonPath("$.actualizadas").value(2));
		get(deudor, "/api/notificaciones/no-leidas/cantidad").andExpect(jsonPath("$.cantidad").value(0));
	}
}
