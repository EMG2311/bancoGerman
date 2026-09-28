package com.bancogerman.demo.cobro;

import java.util.List;

import com.bancogerman.demo.cobro.dto.CrearSolicitudRequest;
import com.bancogerman.demo.cobro.dto.PagarSolicitudRequest;
import com.bancogerman.demo.cobro.dto.SolicitudResponse;
import com.bancogerman.demo.common.ApiException;
import com.bancogerman.demo.movimiento.EstadoMovimiento;
import com.bancogerman.demo.movimiento.MovimientoRepository;
import com.bancogerman.demo.movimiento.MovimientoService;
import com.bancogerman.demo.movimiento.dto.MovimientoResponse;
import com.bancogerman.demo.notificacion.Notificador;
import com.bancogerman.demo.notificacion.TipoNotificacion;
import com.bancogerman.demo.relacion.Relacion;
import com.bancogerman.demo.relacion.RelacionAcceso;
import com.bancogerman.demo.relacion.RelacionGuard;
import com.bancogerman.demo.relacion.Rol;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SolicitudCobroService {

	private final SolicitudCobroRepository solicitudRepository;
	private final MovimientoRepository movimientoRepository;
	private final MovimientoService movimientoService;
	private final RelacionGuard guard;
	private final Notificador notificador;

	@Transactional
	public SolicitudResponse crear(Long relacionId, Long usuarioId, CrearSolicitudRequest request) {
		RelacionAcceso acceso = guard.accederParaModificar(relacionId, usuarioId)
				.exigir(Rol.ACREEDOR, "pedir un cobro");
		Relacion relacion = acceso.relacion();
		relacion.exigirActiva();
		if (request.monto().compareTo(relacion.getSaldo()) > 0) {
			throw ApiException.reglaDeNegocio("No podés pedir más de lo que te deben (" + relacion.getSaldo() + ")");
		}

		SolicitudCobro solicitud = solicitudRepository.save(
				new SolicitudCobro(relacion, request.monto(), request.descripcion()));
		notificador.notificar(acceso.contraparte().getId(), TipoNotificacion.SOLICITUD_COBRO,
				"relacionId", relacionId, "solicitudId", solicitud.getId(), "monto", solicitud.getMonto(),
				"actor", acceso.yo().getNombre());
		return SolicitudResponse.de(solicitud);
	}

	/**
	 * El deudor paga la solicitud: se genera un PAGO por el monto pedido, que sigue las reglas
	 * normales (si la relación requiere confirmación queda pendiente y la solicitud sigue PENDIENTE
	 * hasta que el acreedor confirme).
	 */
	@Transactional
	public MovimientoResponse pagar(Long solicitudId, Long usuarioId, PagarSolicitudRequest request) {
		RelacionAcceso acceso = accederComoDeudor(solicitudId, usuarioId, "pagar una solicitud de cobro");
		acceso.relacion().exigirActiva();
		SolicitudCobro solicitud = pendienteSinPagoEnCurso(solicitudId);
		return movimientoService.crearPago(acceso, solicitud.getMonto(), request.medioPago(),
				"Pago de solicitud de cobro #" + solicitudId, solicitud);
	}

	@Transactional
	public SolicitudResponse rechazar(Long solicitudId, Long usuarioId) {
		RelacionAcceso acceso = accederComoDeudor(solicitudId, usuarioId, "rechazar una solicitud de cobro");
		SolicitudCobro solicitud = pendienteSinPagoEnCurso(solicitudId);
		solicitud.rechazar();
		notificador.notificar(acceso.contraparte().getId(), TipoNotificacion.SOLICITUD_RECHAZADA,
				"relacionId", acceso.relacion().getId(), "solicitudId", solicitudId, "monto", solicitud.getMonto(),
				"actor", acceso.yo().getNombre());
		return SolicitudResponse.de(solicitud);
	}

	@Transactional(readOnly = true)
	public List<SolicitudResponse> listar(Long relacionId, Long usuarioId, EstadoSolicitud estado) {
		guard.acceder(relacionId, usuarioId);
		List<SolicitudCobro> solicitudes = estado == null
				? solicitudRepository.findByRelacionIdOrderByFechaDescIdDesc(relacionId)
				: solicitudRepository.findByRelacionIdAndEstadoOrderByFechaDescIdDesc(relacionId, estado);
		return solicitudes.stream().map(SolicitudResponse::de).toList();
	}

	private RelacionAcceso accederComoDeudor(Long solicitudId, Long usuarioId, String accion) {
		Long relacionId = solicitudRepository.findRelacionIdDe(solicitudId)
				.orElseThrow(() -> ApiException.noEncontrado("Solicitud no encontrada"));
		return guard.accederParaModificar(relacionId, usuarioId).exigir(Rol.DEUDOR, accion);
	}

	private SolicitudCobro pendienteSinPagoEnCurso(Long solicitudId) {
		SolicitudCobro solicitud = solicitudRepository.findById(solicitudId).orElseThrow();
		solicitud.exigirPendiente();
		if (movimientoRepository.existsBySolicitudIdAndEstado(solicitudId, EstadoMovimiento.PENDIENTE)) {
			throw ApiException.conflicto("La solicitud ya tiene un pago esperando confirmación del acreedor");
		}
		return solicitud;
	}
}
