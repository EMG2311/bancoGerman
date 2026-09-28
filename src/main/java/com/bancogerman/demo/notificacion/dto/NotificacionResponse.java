package com.bancogerman.demo.notificacion.dto;

import java.time.Instant;

import com.bancogerman.demo.notificacion.Notificacion;
import com.bancogerman.demo.notificacion.TipoNotificacion;
import com.fasterxml.jackson.annotation.JsonRawValue;

public record NotificacionResponse(Long id, TipoNotificacion tipo, String titulo, @JsonRawValue String payload,
		Instant fecha, boolean leida) {

	public static NotificacionResponse de(Notificacion n) {
		return new NotificacionResponse(n.getId(), n.getTipo(), n.getTipo().titulo(), n.getPayload(), n.getFecha(),
				n.isLeida());
	}
}
