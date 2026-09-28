package com.bancogerman.demo.movimiento.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.bancogerman.demo.movimiento.EstadoMovimiento;
import com.bancogerman.demo.movimiento.MedioPago;
import com.bancogerman.demo.movimiento.Movimiento;
import com.bancogerman.demo.movimiento.TipoMovimiento;

public record MovimientoResponse(Long id, Long relacionId, TipoMovimiento tipo, BigDecimal monto,
		EstadoMovimiento estado, MedioPago medioPago, boolean requirioConfirmacion, Long creadoPorId,
		Long solicitudId, String descripcion, Instant fecha, Instant resueltoEn) {

	public static MovimientoResponse de(Movimiento m) {
		return new MovimientoResponse(m.getId(), m.getRelacion().getId(), m.getTipo(), m.getMonto(), m.getEstado(),
				m.getMedioPago(), m.isRequirioConfirmacion(), m.getCreadoPor().getId(),
				m.getSolicitud() == null ? null : m.getSolicitud().getId(), m.getDescripcion(), m.getFecha(),
				m.getResueltoEn());
	}
}
