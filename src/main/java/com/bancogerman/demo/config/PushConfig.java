package com.bancogerman.demo.config;

import com.bancogerman.demo.dispositivo.DispositivoRepository;
import com.bancogerman.demo.notificacion.push.LogPushSender;
import com.bancogerman.demo.notificacion.push.PushSender;
import com.bancogerman.demo.notificacion.push.expo.ExpoCliente;
import com.bancogerman.demo.notificacion.push.expo.ExpoProperties;
import com.bancogerman.demo.notificacion.push.expo.ExpoPushSender;
import com.bancogerman.demo.notificacion.push.expo.ExpoPushTicketRepository;
import com.bancogerman.demo.notificacion.push.expo.ExpoRecibos;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/**
 * Elige la implementación de push según app.push.proveedor:
 * <ul>
 *   <li>{@code expo}: envío real a través del servicio de push de Expo (default).</li>
 *   <li>{@code log}: no manda nada, solo loguea. Útil sin conexión o sin app instalada.</li>
 * </ul>
 */
@Configuration
public class PushConfig {

	@Configuration
	@ConditionalOnProperty(name = "app.push.proveedor", havingValue = "expo", matchIfMissing = true)
	static class Expo {

		@Bean
		ExpoCliente expoCliente(ExpoProperties props, JsonMapper jsonMapper) {
			return new ExpoCliente(props, jsonMapper);
		}

		@Bean
		PushSender expoPushSender(ExpoCliente cliente, ExpoPushTicketRepository ticketRepository) {
			return new ExpoPushSender(cliente, ticketRepository);
		}

		@Bean
		ExpoRecibos expoRecibos(ExpoCliente cliente, ExpoPushTicketRepository ticketRepository,
				DispositivoRepository dispositivoRepository) {
			return new ExpoRecibos(cliente, ticketRepository, dispositivoRepository);
		}
	}

	@Bean
	@ConditionalOnProperty(name = "app.push.proveedor", havingValue = "log")
	PushSender logPushSender() {
		return new LogPushSender();
	}
}
