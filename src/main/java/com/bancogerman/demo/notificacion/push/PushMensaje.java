package com.bancogerman.demo.notificacion.push;

import java.util.Map;

import com.bancogerman.demo.dispositivo.Plataforma;

public record PushMensaje(String token, Plataforma plataforma, String titulo, String cuerpo,
		Map<String, String> datos) {
}
