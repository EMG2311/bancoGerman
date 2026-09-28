package com.bancogerman.demo.relacion.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.bancogerman.demo.relacion.EstadoRelacion;
import com.bancogerman.demo.relacion.Relacion;
import com.bancogerman.demo.relacion.Rol;
import com.bancogerman.demo.usuario.dto.UsuarioResumen;

/**
 * @param miRol                 lado del usuario logueado en esta relación
 * @param pendienteConfirmacion suma de pagos esperando que el acreedor los confirme
 * @param disponibleParaPagar   cuánto se puede pagar todavía sin pasarse de la deuda
 */
public record RelacionResponse(Long id, Rol miRol, UsuarioResumen acreedor, UsuarioResumen deudor,
		BigDecimal saldo, BigDecimal pendienteConfirmacion, BigDecimal disponibleParaPagar,
		boolean requiereConfirmacion, EstadoRelacion estado, Instant creadoEn, Instant respondidaEn,
		Instant eliminadaEn) {

	public static RelacionResponse de(Relacion r, Rol miRol, BigDecimal pendienteConfirmacion) {
		return new RelacionResponse(r.getId(), miRol, UsuarioResumen.de(r.getAcreedor()),
				UsuarioResumen.de(r.getDeudor()), r.getSaldo(), pendienteConfirmacion,
				r.getSaldo().subtract(pendienteConfirmacion).max(BigDecimal.ZERO), r.isRequiereConfirmacion(),
				r.getEstado(), r.getCreadoEn(), r.getRespondidaEn(), r.getEliminadaEn());
	}
}
