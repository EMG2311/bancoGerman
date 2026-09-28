package com.bancogerman.demo.notificacion.push;

import java.util.Map;

/** Notificación ya guardada en el historial, lista para mandarse a los dispositivos del destinatario. */
public record PushPendiente(Long destinatarioId, String titulo, String cuerpo, Map<String, String> datos) {
}
