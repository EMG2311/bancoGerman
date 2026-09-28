package com.bancogerman.demo.notificacion;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Punto de entrada para que los servicios de negocio pidan una notificación. */
@Component
@RequiredArgsConstructor
public class Notificador {

	private final ApplicationEventPublisher publisher;

	/**
	 * @param datos pares clave/valor (clave1, valor1, clave2, valor2, ...) que viajan en el payload
	 */
	public void notificar(Long destinatarioId, TipoNotificacion tipo, Object... datos) {
		if (datos.length % 2 != 0) {
			throw new IllegalArgumentException("Los datos van en pares clave/valor");
		}
		Map<String, Object> mapa = new LinkedHashMap<>();
		for (int i = 0; i < datos.length; i += 2) {
			if (datos[i + 1] != null) {
				mapa.put((String) datos[i], datos[i + 1]);
			}
		}
		publisher.publishEvent(new NotificacionEvento(destinatarioId, tipo, mapa));
	}
}
