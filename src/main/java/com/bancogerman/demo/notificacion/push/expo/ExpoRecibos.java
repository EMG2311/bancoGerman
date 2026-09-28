package com.bancogerman.demo.notificacion.push.expo;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.bancogerman.demo.dispositivo.DispositivoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import tools.jackson.databind.JsonNode;

/**
 * Expo recomienda revisar los recibos ~15 minutos después de enviar: ahí aparece si FCM/APNs
 * entregaron el push o si el token ya no existe (app desinstalada) y hay que borrar el dispositivo.
 * Los recibos vencen a las 24 h.
 */
@Slf4j
public class ExpoRecibos {

	static final Duration ESPERA = Duration.ofMinutes(15);
	static final Duration VENCIMIENTO = Duration.ofHours(24);

	private final ExpoCliente cliente;
	private final ExpoPushTicketRepository ticketRepository;
	private final DispositivoRepository dispositivoRepository;

	public ExpoRecibos(ExpoCliente cliente, ExpoPushTicketRepository ticketRepository,
			DispositivoRepository dispositivoRepository) {
		this.cliente = cliente;
		this.ticketRepository = ticketRepository;
		this.dispositivoRepository = dispositivoRepository;
	}

	@Scheduled(initialDelayString = "PT1M", fixedDelayString = "${app.push.expo.intervalo-recibos:PT5M}")
	public void revisar() {
		Instant ahora = Instant.now();
		ticketRepository.borrarAnterioresA(ahora.minus(VENCIMIENTO));

		List<ExpoPushTicket> tickets = ticketRepository.findTop1000ByCreadoEnBeforeOrderByCreadoEnAsc(ahora.minus(ESPERA));
		if (tickets.isEmpty()) {
			return;
		}
		List<String> ids = tickets.stream().map(ExpoPushTicket::getTicketId).toList();
		cliente.post("/push/getReceipts", Map.of("ids", ids))
				.ifPresent(respuesta -> procesar(tickets, respuesta.path("data")));
	}

	private void procesar(List<ExpoPushTicket> tickets, JsonNode recibos) {
		List<ExpoPushTicket> resueltos = new ArrayList<>();
		for (ExpoPushTicket ticket : tickets) {
			JsonNode recibo = recibos.path(ticket.getTicketId());
			if (recibo.isMissingNode()) {
				continue; // Todavía no está listo; se reintenta en la próxima vuelta
			}
			resueltos.add(ticket);
			if (!recibo.path("status").asString("").equals("error")) {
				continue;
			}
			String error = recibo.path("details").path("error").asString("");
			switch (error) {
				case "DeviceNotRegistered" -> {
					log.info("El dispositivo con token {} ya no existe, se elimina", ticket.getPushToken());
					dispositivoRepository.borrarPorToken(ticket.getPushToken());
				}
				case "InvalidCredentials" -> log.error("Expo no pudo entregar el push: faltan o son inválidas las "
						+ "credenciales de FCM/APNs del proyecto en EAS ({})", recibo.path("message").asString(""));
				default -> log.warn("Push no entregado ({}): {}", error, recibo.path("message").asString(""));
			}
		}
		ticketRepository.deleteAll(resueltos);
	}
}
