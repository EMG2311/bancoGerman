package com.bancogerman.demo.movimiento;

import java.math.BigDecimal;

import com.bancogerman.demo.cobro.SolicitudCobro;
import com.bancogerman.demo.common.ApiException;
import com.bancogerman.demo.common.Pagina;
import com.bancogerman.demo.movimiento.dto.CargoRequest;
import com.bancogerman.demo.movimiento.dto.MovimientoResponse;
import com.bancogerman.demo.movimiento.dto.PagoRequest;
import com.bancogerman.demo.movimiento.dto.SaldoResponse;
import com.bancogerman.demo.notificacion.Notificador;
import com.bancogerman.demo.notificacion.TipoNotificacion;
import com.bancogerman.demo.relacion.Relacion;
import com.bancogerman.demo.relacion.RelacionAcceso;
import com.bancogerman.demo.relacion.RelacionGuard;
import com.bancogerman.demo.relacion.Rol;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Único lugar donde cambia el saldo de una relación. Toda operación bloquea primero la fila de la
 * relación (SELECT ... FOR UPDATE), así dos pagos simultáneos no pueden pasar el saldo por debajo de 0.
 */
@Service
@RequiredArgsConstructor
public class MovimientoService {

	private final MovimientoRepository movimientoRepository;
	private final RelacionGuard guard;
	private final Notificador notificador;

	/** Los cargos los crea el acreedor y nunca requieren confirmación. */
	@Transactional
	public MovimientoResponse registrarCargo(Long relacionId, Long usuarioId, CargoRequest request) {
		RelacionAcceso acceso = guard.accederParaModificar(relacionId, usuarioId)
				.exigir(Rol.ACREEDOR, "registrar cargos");
		Relacion relacion = acceso.relacion();
		relacion.exigirActiva();

		Movimiento cargo = movimientoRepository.save(
				Movimiento.cargo(relacion, acceso.yo(), request.monto(), request.descripcion()));
		relacion.aplicarCargo(cargo.getMonto());

		notificador.notificar(acceso.contraparte().getId(), TipoNotificacion.CARGO_REGISTRADO,
				"relacionId", relacionId, "movimientoId", cargo.getId(), "monto", cargo.getMonto(),
				"actor", acceso.yo().getNombre());
		return MovimientoResponse.de(cargo);
	}

	/** Lo puede registrar el deudor o el acreedor (pago recibido por fuera de la app). */
	@Transactional
	public MovimientoResponse registrarPago(Long relacionId, Long usuarioId, PagoRequest request) {
		RelacionAcceso acceso = guard.accederParaModificar(relacionId, usuarioId);
		acceso.relacion().exigirActiva();
		return crearPago(acceso, request.monto(), request.medioPago(), request.descripcion(), null);
	}

	/**
	 * Reglas de un pago nuevo:
	 * <ul>
	 *   <li>No puede superar lo que queda por pagar: saldo − pagos ya pendientes de confirmación.</li>
	 *   <li>Si lo registra el acreedor, nace confirmado (él mismo dice que recibió la plata).</li>
	 *   <li>Si lo registra el deudor, nace pendiente o confirmado según el flag actual de la relación.</li>
	 * </ul>
	 * Requiere que la relación ya esté bloqueada por el llamador.
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public MovimientoResponse crearPago(RelacionAcceso acceso, BigDecimal monto, MedioPago medio,
			String descripcion, SolicitudCobro solicitud) {
		Relacion relacion = acceso.relacion();
		BigDecimal disponible = relacion.getSaldo().subtract(movimientoRepository.sumarPagosPendientes(relacion.getId()));
		if (monto.compareTo(disponible) > 0) {
			throw ApiException.reglaDeNegocio("El pago de " + monto + " supera lo que queda por pagar ("
					+ disponible.max(BigDecimal.ZERO) + ")");
		}

		boolean requiereConfirmacion = acceso.rol() == Rol.DEUDOR && relacion.isRequiereConfirmacion();
		Movimiento pago = movimientoRepository.save(Movimiento.pago(relacion, acceso.yo(), monto, medio,
				requiereConfirmacion, solicitud, descripcion));

		if (pago.estaConfirmado()) {
			relacion.aplicarPago(pago.getMonto());
			if (solicitud != null) {
				solicitud.marcarPagada();
			}
		}

		notificador.notificar(acceso.contraparte().getId(),
				requiereConfirmacion ? TipoNotificacion.PAGO_PENDIENTE : TipoNotificacion.PAGO_REGISTRADO,
				"relacionId", relacion.getId(), "movimientoId", pago.getId(), "monto", pago.getMonto(),
				"medioPago", medio.name().toLowerCase(), "actor", acceso.yo().getNombre());
		return MovimientoResponse.de(pago);
	}

	@Transactional
	public MovimientoResponse confirmar(Long movimientoId, Long usuarioId) {
		Movimiento pago = pagoPendienteDelAcreedor(movimientoId, usuarioId, "confirmar pagos");
		Relacion relacion = pago.getRelacion();

		pago.confirmar();
		relacion.aplicarPago(pago.getMonto());
		if (pago.getSolicitud() != null) {
			pago.getSolicitud().marcarPagada();
		}

		notificador.notificar(relacion.getDeudor().getId(), TipoNotificacion.PAGO_CONFIRMADO,
				"relacionId", relacion.getId(), "movimientoId", pago.getId(), "monto", pago.getMonto(),
				"actor", relacion.getAcreedor().getNombre());
		return MovimientoResponse.de(pago);
	}

	@Transactional
	public MovimientoResponse rechazar(Long movimientoId, Long usuarioId) {
		Movimiento pago = pagoPendienteDelAcreedor(movimientoId, usuarioId, "rechazar pagos");
		Relacion relacion = pago.getRelacion();

		pago.rechazar();

		notificador.notificar(relacion.getDeudor().getId(), TipoNotificacion.PAGO_RECHAZADO,
				"relacionId", relacion.getId(), "movimientoId", pago.getId(), "monto", pago.getMonto(),
				"actor", relacion.getAcreedor().getNombre());
		return MovimientoResponse.de(pago);
	}

	@Transactional(readOnly = true)
	public Pagina<MovimientoResponse> listar(Long relacionId, Long usuarioId, EstadoMovimiento estado, int pagina,
			int tamanio) {
		guard.acceder(relacionId, usuarioId);
		PageRequest pageable = PageRequest.of(pagina, tamanio);
		var page = estado == null
				? movimientoRepository.findByRelacionIdOrderByFechaDescIdDesc(relacionId, pageable)
				: movimientoRepository.findByRelacionIdAndEstadoOrderByFechaDescIdDesc(relacionId, estado, pageable);
		return Pagina.de(page, MovimientoResponse::de);
	}

	/** Saldo cacheado vs. reconstruido desde el ledger (deberían coincidir siempre). */
	@Transactional(readOnly = true)
	public SaldoResponse saldo(Long relacionId, Long usuarioId) {
		Relacion relacion = guard.acceder(relacionId, usuarioId).relacion();
		BigDecimal pendiente = movimientoRepository.sumarPagosPendientes(relacionId);
		BigDecimal ledger = movimientoRepository.saldoSegunLedger(relacionId);
		return new SaldoResponse(relacion.getSaldo(), ledger, pendiente,
				relacion.getSaldo().subtract(pendiente).max(BigDecimal.ZERO),
				relacion.getSaldo().compareTo(ledger) == 0);
	}

	/**
	 * Bloquea la relación ANTES de leer el movimiento, así el estado que vemos no puede cambiar
	 * por una confirmación concurrente.
	 */
	private Movimiento pagoPendienteDelAcreedor(Long movimientoId, Long usuarioId, String accion) {
		Long relacionId = movimientoRepository.findRelacionIdDe(movimientoId)
				.orElseThrow(() -> ApiException.noEncontrado("Movimiento no encontrado"));
		guard.accederParaModificar(relacionId, usuarioId).exigir(Rol.ACREEDOR, accion);
		Movimiento movimiento = movimientoRepository.findById(movimientoId).orElseThrow();
		if (movimiento.getTipo() != TipoMovimiento.PAGO) {
			throw ApiException.conflicto("Solo los pagos se confirman o rechazan");
		}
		return movimiento;
	}
}
