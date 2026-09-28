package com.bancogerman.demo.notificacion.push;

import com.bancogerman.demo.dispositivo.Dispositivo;
import com.bancogerman.demo.dispositivo.DispositivoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Manda un push a todos los dispositivos del destinatario, en un hilo aparte: el request que
 * originó la notificación no espera al proveedor, y un proveedor lento o caído no afecta a la API.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PushDespachador {

	private final DispositivoRepository dispositivoRepository;
	private final PushSender pushSender;

	@Async
	public void enviar(PushPendiente push) {
		for (Dispositivo dispositivo : dispositivoRepository.findByUsuarioId(push.destinatarioId())) {
			try {
				ResultadoPush resultado = pushSender.enviar(new PushMensaje(dispositivo.getPushToken(),
						dispositivo.getPlataforma(), push.titulo(), push.cuerpo(), push.datos()));
				if (resultado == ResultadoPush.TOKEN_INVALIDO) {
					log.info("Token push inválido, se elimina el dispositivo {}", dispositivo.getId());
					dispositivoRepository.deleteById(dispositivo.getId());
				}
			} catch (RuntimeException e) {
				log.warn("Falló el push al dispositivo {}", dispositivo.getId(), e);
			}
		}
	}
}
