package com.bancogerman.demo.cobro.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.bancogerman.demo.cobro.EstadoSolicitud;
import com.bancogerman.demo.cobro.SolicitudCobro;

public record SolicitudResponse(Long id, Long relacionId, BigDecimal monto, EstadoSolicitud estado,
		String descripcion, Instant fecha, Instant respondidaEn) {

	public static SolicitudResponse de(SolicitudCobro s) {
		return new SolicitudResponse(s.getId(), s.getRelacion().getId(), s.getMonto(), s.getEstado(),
				s.getDescripcion(), s.getFecha(), s.getRespondidaEn());
	}
}
