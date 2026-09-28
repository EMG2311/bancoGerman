package com.bancogerman.demo.movimiento;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MovimientoRepository extends JpaRepository<Movimiento, Long> {

	Page<Movimiento> findByRelacionIdOrderByFechaDescIdDesc(Long relacionId, Pageable pageable);

	Page<Movimiento> findByRelacionIdAndEstadoOrderByFechaDescIdDesc(Long relacionId, EstadoMovimiento estado,
			Pageable pageable);

	List<Movimiento> findByRelacionIdAndEstado(Long relacionId, EstadoMovimiento estado);

	boolean existsBySolicitudIdAndEstado(Long solicitudId, EstadoMovimiento estado);

	@Query("select m.relacion.id from Movimiento m where m.id = :movimientoId")
	Optional<Long> findRelacionIdDe(Long movimientoId);

	@Query("""
			select coalesce(sum(m.monto), 0) from Movimiento m
			where m.relacion.id = :relacionId and m.tipo = :tipo and m.estado = :estado""")
	BigDecimal sumar(Long relacionId, TipoMovimiento tipo, EstadoMovimiento estado);

	default BigDecimal sumarPagosPendientes(Long relacionId) {
		return sumar(relacionId, TipoMovimiento.PAGO, EstadoMovimiento.PENDIENTE);
	}

	/** Saldo reconstruido desde el ledger: Σ cargos confirmados − Σ pagos confirmados. */
	default BigDecimal saldoSegunLedger(Long relacionId) {
		return sumar(relacionId, TipoMovimiento.CARGO, EstadoMovimiento.CONFIRMADO)
				.subtract(sumar(relacionId, TipoMovimiento.PAGO, EstadoMovimiento.CONFIRMADO));
	}

	@Query("""
			select m.relacion.id, sum(m.monto) from Movimiento m
			where m.relacion.id in :relacionIds
			  and m.tipo = com.bancogerman.demo.movimiento.TipoMovimiento.PAGO
			  and m.estado = com.bancogerman.demo.movimiento.EstadoMovimiento.PENDIENTE
			group by m.relacion.id""")
	List<Object[]> sumarPagosPendientesAgrupado(Collection<Long> relacionIds);

	default Map<Long, BigDecimal> sumarPagosPendientesPorRelacion(Collection<Long> relacionIds) {
		if (relacionIds.isEmpty()) {
			return Map.of();
		}
		return sumarPagosPendientesAgrupado(relacionIds).stream()
				.collect(Collectors.toMap(fila -> (Long) fila[0], fila -> (BigDecimal) fila[1]));
	}
}
