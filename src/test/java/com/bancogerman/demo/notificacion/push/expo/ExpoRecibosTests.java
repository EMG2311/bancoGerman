package com.bancogerman.demo.notificacion.push.expo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.bancogerman.demo.dispositivo.DispositivoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

class ExpoRecibosTests {

	private final JsonMapper jsonMapper = JsonMapper.builder().build();
	private final ExpoPushTicketRepository tickets = mock(ExpoPushTicketRepository.class);
	private final DispositivoRepository dispositivos = mock(DispositivoRepository.class);
	private ExpoFalso expo;
	private ExpoRecibos recibos;

	@BeforeEach
	void setUp() throws Exception {
		expo = new ExpoFalso();
		recibos = new ExpoRecibos(new ExpoCliente(new ExpoProperties(expo.url(), null), jsonMapper), tickets,
				dispositivos);
	}

	@AfterEach
	void tearDown() {
		expo.close();
	}

	@Test
	@SuppressWarnings("unchecked")
	void borraElDispositivoCuyoTokenYaNoExisteYSacaLosTicketsResueltos() {
		ExpoPushTicket entregado = new ExpoPushTicket("t-ok", "ExponentPushToken[a]");
		ExpoPushTicket desinstalado = new ExpoPushTicket("t-baja", "ExponentPushToken[b]");
		ExpoPushTicket sinRecibo = new ExpoPushTicket("t-espera", "ExponentPushToken[c]");
		when(tickets.findTop1000ByCreadoEnBeforeOrderByCreadoEnAsc(any()))
				.thenReturn(List.of(entregado, desinstalado, sinRecibo));
		expo.respuesta = """
				{"data":{
				  "t-ok":{"status":"ok"},
				  "t-baja":{"status":"error","message":"gone","details":{"error":"DeviceNotRegistered"}}
				}}""";

		recibos.revisar();

		assertThat(expo.pedidos.getFirst().ruta()).isEqualTo("/push/getReceipts");
		assertThat(expo.pedidos.getFirst().cuerpo()).contains("t-ok", "t-baja", "t-espera");
		verify(dispositivos).borrarPorToken("ExponentPushToken[b]");
		verify(dispositivos, never()).borrarPorToken("ExponentPushToken[a]");

		ArgumentCaptor<List<ExpoPushTicket>> borrados = ArgumentCaptor.forClass(List.class);
		verify(tickets).deleteAll(borrados.capture());
		// El que todavía no tiene recibo se reintenta en la próxima vuelta
		assertThat(borrados.getValue()).containsExactly(entregado, desinstalado);
	}

	@Test
	void sinTicketsNoLlamaAExpo() {
		when(tickets.findTop1000ByCreadoEnBeforeOrderByCreadoEnAsc(any())).thenReturn(List.of());

		recibos.revisar();

		assertThat(expo.pedidos).isEmpty();
		verify(tickets).borrarAnterioresA(any());
	}

	@Test
	void siExpoFallaNoBorraNada() {
		when(tickets.findTop1000ByCreadoEnBeforeOrderByCreadoEnAsc(any()))
				.thenReturn(List.of(new ExpoPushTicket("t-1", "ExponentPushToken[a]")));
		expo.status = 503;

		recibos.revisar();

		verify(tickets, never()).deleteAll(any());
		verify(dispositivos, never()).borrarPorToken(any());
	}
}
