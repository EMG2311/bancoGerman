package com.bancogerman.demo.notificacion;

import java.util.Map;

/**
 * Se publica dentro de la transacción de negocio y se procesa recién después del commit
 * (ver {@link NotificacionListener}), así un rollback nunca manda un push.
 */
public record NotificacionEvento(Long destinatarioId, TipoNotificacion tipo, Map<String, Object> datos) {
}
