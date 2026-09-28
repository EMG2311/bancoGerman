package com.bancogerman.demo.notificacion.push;

import lombok.extern.slf4j.Slf4j;

/** Implementación de desarrollo: no manda nada, solo loguea. */
@Slf4j
public class LogPushSender implements PushSender {

	@Override
	public ResultadoPush enviar(PushMensaje mensaje) {
		log.info("[PUSH {}] {} -> {}: {} {}", mensaje.plataforma(), abreviar(mensaje.token()), mensaje.titulo(),
				mensaje.cuerpo(), mensaje.datos());
		return ResultadoPush.ENVIADO;
	}

	private static String abreviar(String token) {
		return token.length() <= 12 ? token : token.substring(0, 12) + "...";
	}
}
