package com.bancogerman.demo.notificacion.push.expo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.bancogerman.demo.notificacion.push.PushMensaje;
import com.bancogerman.demo.notificacion.push.PushSender;
import com.bancogerman.demo.notificacion.push.ResultadoPush;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;

/**
 * Manda push a través del servicio de Expo, que los entrega por FCM (Android) o APNs (iOS).
 * La app obtiene el token con Notifications.getExpoPushTokenAsync() y lo registra en /api/dispositivos.
 *
 * @see <a href="https://docs.expo.dev/push-notifications/sending-notifications/">Expo push API</a>
 */
@Slf4j
public class ExpoPushSender implements PushSender {

	/** Canal de Android que crea la app. Si no coincide, Android usa el canal por defecto. */
	static final String CANAL_ANDROID = "movimientos";

	private static final Pattern TOKEN_EXPO = Pattern.compile("^Expo(nent)?PushToken\\[.+]$");

	private final ExpoCliente cliente;
	private final ExpoPushTicketRepository ticketRepository;

	public ExpoPushSender(ExpoCliente cliente, ExpoPushTicketRepository ticketRepository) {
		this.cliente = cliente;
		this.ticketRepository = ticketRepository;
	}

	@Override
	public ResultadoPush enviar(PushMensaje mensaje) {
		if (!TOKEN_EXPO.matcher(mensaje.token()).matches()) {
			log.warn("El token {} no es un token de Expo, se descarta", mensaje.token());
			return ResultadoPush.TOKEN_INVALIDO;
		}

		Map<String, Object> push = new LinkedHashMap<>();
		push.put("to", mensaje.token());
		push.put("title", mensaje.titulo());
		push.put("body", mensaje.cuerpo());
		push.put("data", mensaje.datos());
		push.put("sound", "default");
		push.put("priority", "high");
		push.put("channelId", CANAL_ANDROID);

		// Se manda como array de 1 para que la respuesta siempre sea { data: [ticket] }
		return cliente.post("/push/send", List.of(push))
				.map(respuesta -> interpretarTicket(respuesta.path("data").path(0), mensaje.token()))
				.orElse(ResultadoPush.ERROR);
	}

	private ResultadoPush interpretarTicket(JsonNode ticket, String token) {
		String estado = ticket.path("status").asString("");
		if (estado.equals("ok")) {
			// El push fue aceptado, pero la entrega real se confirma en el recibo (ver ExpoRecibos)
			ticketRepository.save(new ExpoPushTicket(ticket.path("id").asString(""), token));
			return ResultadoPush.ENVIADO;
		}
		String error = ticket.path("details").path("error").asString("");
		if (error.equals("DeviceNotRegistered")) {
			return ResultadoPush.TOKEN_INVALIDO;
		}
		log.warn("Expo rechazó el push: {} ({})", ticket.path("message").asString(""), error);
		return ResultadoPush.ERROR;
	}
}
