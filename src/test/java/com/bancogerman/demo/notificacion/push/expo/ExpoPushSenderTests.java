package com.bancogerman.demo.notificacion.push.expo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Map;

import com.bancogerman.demo.dispositivo.Plataforma;
import com.bancogerman.demo.notificacion.push.PushMensaje;
import com.bancogerman.demo.notificacion.push.ResultadoPush;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class ExpoPushSenderTests {

	private static final String TOKEN = "ExponentPushToken[abc123]";

	private final JsonMapper jsonMapper = JsonMapper.builder().build();
	private final ExpoPushTicketRepository tickets = mock(ExpoPushTicketRepository.class);
	private ExpoFalso expo;
	private ExpoPushSender sender;

	@BeforeEach
	void setUp() throws Exception {
		expo = new ExpoFalso();
		sender = new ExpoPushSender(new ExpoCliente(new ExpoProperties(expo.url(), "secreto"), jsonMapper), tickets);
	}

	@AfterEach
	void tearDown() {
		expo.close();
	}

	private PushMensaje mensaje(String token) {
		return new PushMensaje(token, Plataforma.ANDROID, "Pago para confirmar", "Ana registró un pago de $100.00",
				Map.of("relacionId", "7", "tipo", "PAGO_PENDIENTE"));
	}

	@Test
	void mandaElFormatoDeExpoYGuardaElTicket() {
		expo.respuesta = """
				{"data":[{"status":"ok","id":"ticket-1"}]}""";

		assertThat(sender.enviar(mensaje(TOKEN))).isEqualTo(ResultadoPush.ENVIADO);

		ExpoFalso.Pedido pedido = expo.pedidos.getFirst();
		assertThat(pedido.ruta()).isEqualTo("/push/send");
		assertThat(pedido.authorization()).isEqualTo("Bearer secreto");
		JsonNode push = jsonMapper.readTree(pedido.cuerpo()).path(0);
		assertThat(push.path("to").asString()).isEqualTo(TOKEN);
		assertThat(push.path("title").asString()).isEqualTo("Pago para confirmar");
		assertThat(push.path("body").asString()).isEqualTo("Ana registró un pago de $100.00");
		assertThat(push.path("data").path("relacionId").asString()).isEqualTo("7");
		assertThat(push.path("channelId").asString()).isEqualTo("movimientos");
		assertThat(push.path("priority").asString()).isEqualTo("high");

		ArgumentCaptor<ExpoPushTicket> ticket = ArgumentCaptor.forClass(ExpoPushTicket.class);
		verify(tickets).save(ticket.capture());
		assertThat(ticket.getValue().getTicketId()).isEqualTo("ticket-1");
		assertThat(ticket.getValue().getPushToken()).isEqualTo(TOKEN);
	}

	@Test
	void deviceNotRegisteredEsTokenInvalido() {
		expo.respuesta = """
				{"data":[{"status":"error","message":"not registered","details":{"error":"DeviceNotRegistered"}}]}""";

		assertThat(sender.enviar(mensaje(TOKEN))).isEqualTo(ResultadoPush.TOKEN_INVALIDO);
		verify(tickets, never()).save(any());
	}

	@Test
	void otrosErroresDelTicketSonError() {
		expo.respuesta = """
				{"data":[{"status":"error","message":"too big","details":{"error":"MessageTooBig"}}]}""";

		assertThat(sender.enviar(mensaje(TOKEN))).isEqualTo(ResultadoPush.ERROR);
	}

	@Test
	void unaFallaHttpEsError() {
		expo.status = 500;

		assertThat(sender.enviar(mensaje(TOKEN))).isEqualTo(ResultadoPush.ERROR);
	}

	@Test
	void unTokenQueNoEsDeExpoNiSeManda() {
		assertThat(sender.enviar(mensaje("token-fcm-crudo"))).isEqualTo(ResultadoPush.TOKEN_INVALIDO);
		assertThat(expo.pedidos).isEmpty();
	}

	@Test
	void sinExpoDisponibleEsError() {
		ExpoPushSender sinServidor = new ExpoPushSender(
				new ExpoCliente(new ExpoProperties("http://127.0.0.1:1", null), jsonMapper), tickets);

		assertThat(sinServidor.enviar(mensaje(TOKEN))).isEqualTo(ResultadoPush.ERROR);
	}
}
