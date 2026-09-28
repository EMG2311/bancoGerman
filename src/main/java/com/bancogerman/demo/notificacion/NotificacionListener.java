package com.bancogerman.demo.notificacion;

import com.bancogerman.demo.notificacion.push.PushDespachador;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificacionListener {

	private final NotificacionService notificacionService;
	private final PushDespachador pushDespachador;

	/**
	 * Corre después del commit de la operación de negocio: el historial se guarda en el acto
	 * (así la app ya lo ve al refrescar) y el push sale en segundo plano. Si algo falla acá no se
	 * propaga: la operación ya está confirmada y el cliente no tiene que ver un error.
	 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void alConfirmarse(NotificacionEvento evento) {
		try {
			pushDespachador.enviar(notificacionService.registrar(evento));
		} catch (RuntimeException e) {
			log.error("No se pudo registrar la notificación {} para el usuario {}", evento.tipo(),
					evento.destinatarioId(), e);
		}
	}
}
