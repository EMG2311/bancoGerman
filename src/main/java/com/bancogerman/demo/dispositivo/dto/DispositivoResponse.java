package com.bancogerman.demo.dispositivo.dto;

import java.time.Instant;

import com.bancogerman.demo.dispositivo.Dispositivo;
import com.bancogerman.demo.dispositivo.Plataforma;

public record DispositivoResponse(Long id, Plataforma plataforma, Instant creadoEn, Instant actualizadoEn) {

	public static DispositivoResponse de(Dispositivo d) {
		return new DispositivoResponse(d.getId(), d.getPlataforma(), d.getCreadoEn(), d.getActualizadoEn());
	}
}
