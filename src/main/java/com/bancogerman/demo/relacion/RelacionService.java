package com.bancogerman.demo.relacion;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.bancogerman.demo.cobro.EstadoSolicitud;
import com.bancogerman.demo.cobro.SolicitudCobroRepository;
import com.bancogerman.demo.common.ApiException;
import com.bancogerman.demo.movimiento.EstadoMovimiento;
import com.bancogerman.demo.movimiento.MovimientoRepository;
import com.bancogerman.demo.notificacion.Notificador;
import com.bancogerman.demo.notificacion.TipoNotificacion;
import com.bancogerman.demo.relacion.dto.InvitarRequest;
import com.bancogerman.demo.relacion.dto.RelacionResponse;
import com.bancogerman.demo.usuario.Usuario;
import com.bancogerman.demo.usuario.UsuarioRepository;
import com.bancogerman.demo.usuario.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RelacionService {

	private static final Set<EstadoRelacion> VIGENTES = EnumSet.of(EstadoRelacion.INVITADA, EstadoRelacion.ACTIVA);

	private final RelacionRepository relacionRepository;
	private final UsuarioRepository usuarioRepository;
	private final MovimientoRepository movimientoRepository;
	private final SolicitudCobroRepository solicitudCobroRepository;
	private final RelacionGuard guard;
	private final Notificador notificador;

	/** El usuario logueado invita a otro (por email) a ser su deudor. */
	@Transactional
	public RelacionResponse invitar(Long acreedorId, InvitarRequest request) {
		Usuario acreedor = usuarioRepository.findById(acreedorId)
				.orElseThrow(() -> ApiException.noEncontrado("Usuario no encontrado"));
		Usuario deudor = usuarioRepository.findByEmail(UsuarioService.normalizarEmail(request.emailDeudor()))
				.orElseThrow(() -> ApiException.noEncontrado("No hay ningún usuario registrado con ese email"));
		if (deudor.getId().equals(acreedorId)) {
			throw ApiException.reglaDeNegocio("No podés ser tu propio deudor");
		}
		if (relacionRepository.existsByAcreedorIdAndDeudorIdAndEstadoIn(acreedorId, deudor.getId(), VIGENTES)) {
			throw ApiException.conflicto("Ya tenés una relación vigente o una invitación pendiente con ese usuario");
		}
		Relacion relacion = relacionRepository.save(
				new Relacion(acreedor, deudor, Boolean.TRUE.equals(request.requiereConfirmacion())));
		notificador.notificar(deudor.getId(), TipoNotificacion.INVITACION_RELACION,
				"relacionId", relacion.getId(), "actor", acreedor.getNombre());
		return RelacionResponse.de(relacion, Rol.ACREEDOR, BigDecimal.ZERO);
	}

	@Transactional
	public RelacionResponse aceptar(Long relacionId, Long usuarioId) {
		RelacionAcceso acceso = guard.accederParaModificar(relacionId, usuarioId)
				.exigir(Rol.DEUDOR, "aceptar la invitación");
		acceso.relacion().aceptar();
		notificador.notificar(acceso.contraparte().getId(), TipoNotificacion.RELACION_ACEPTADA,
				"relacionId", relacionId, "actor", acceso.yo().getNombre());
		return respuesta(acceso);
	}

	@Transactional
	public RelacionResponse rechazar(Long relacionId, Long usuarioId) {
		RelacionAcceso acceso = guard.accederParaModificar(relacionId, usuarioId)
				.exigir(Rol.DEUDOR, "rechazar la invitación");
		acceso.relacion().rechazar();
		notificador.notificar(acceso.contraparte().getId(), TipoNotificacion.RELACION_RECHAZADA,
				"relacionId", relacionId, "actor", acceso.yo().getNombre());
		return respuesta(acceso);
	}

	@Transactional
	public RelacionResponse cambiarConfirmacion(Long relacionId, Long usuarioId, boolean requiere) {
		RelacionAcceso acceso = guard.accederParaModificar(relacionId, usuarioId)
				.exigir(Rol.ACREEDOR, "cambiar si los pagos requieren confirmación");
		acceso.relacion().cambiarRequiereConfirmacion(requiere);
		return respuesta(acceso);
	}

	/**
	 * Borrado lógico. El acreedor puede eliminarla siempre (condona lo que quede). El deudor solo
	 * cuando no debe nada, para que no pueda "borrar" su deuda. Los pagos y solicitudes que
	 * estaban pendientes quedan rechazados.
	 */
	@Transactional
	public void eliminar(Long relacionId, Long usuarioId) {
		RelacionAcceso acceso = guard.accederParaModificar(relacionId, usuarioId);
		Relacion relacion = acceso.relacion();
		if (acceso.rol() == Rol.DEUDOR && relacion.getSaldo().signum() > 0) {
			throw ApiException.reglaDeNegocio("No podés eliminar una relación en la que todavía debés dinero");
		}
		relacion.eliminar();
		movimientoRepository.findByRelacionIdAndEstado(relacionId, EstadoMovimiento.PENDIENTE)
				.forEach(m -> m.rechazar());
		solicitudCobroRepository.findByRelacionIdAndEstado(relacionId, EstadoSolicitud.PENDIENTE)
				.forEach(s -> s.rechazar());
		notificador.notificar(acceso.contraparte().getId(), TipoNotificacion.RELACION_ELIMINADA,
				"relacionId", relacionId, "actor", acceso.yo().getNombre());
	}

	@Transactional(readOnly = true)
	public RelacionResponse obtener(Long relacionId, Long usuarioId) {
		return respuesta(guard.acceder(relacionId, usuarioId));
	}

	/**
	 * @param rol    null = ambos lados
	 * @param estado null = solo las vigentes (invitadas y activas)
	 */
	@Transactional(readOnly = true)
	public List<RelacionResponse> listar(Long usuarioId, Rol rol, EstadoRelacion estado) {
		Set<EstadoRelacion> estados = estado == null ? VIGENTES : EnumSet.of(estado);
		List<Relacion> relaciones = new ArrayList<>();
		if (rol == null || rol == Rol.ACREEDOR) {
			relaciones.addAll(relacionRepository.findComoAcreedor(usuarioId, estados));
		}
		if (rol == null || rol == Rol.DEUDOR) {
			relaciones.addAll(relacionRepository.findComoDeudor(usuarioId, estados));
		}
		relaciones.sort(Comparator.comparing(Relacion::getCreadoEn).reversed());

		Map<Long, BigDecimal> pendientes = movimientoRepository
				.sumarPagosPendientesPorRelacion(relaciones.stream().map(Relacion::getId).toList());
		return relaciones.stream()
				.map(r -> RelacionResponse.de(r, r.rolDe(usuarioId).orElseThrow(),
						pendientes.getOrDefault(r.getId(), BigDecimal.ZERO)))
				.toList();
	}

	private RelacionResponse respuesta(RelacionAcceso acceso) {
		return RelacionResponse.de(acceso.relacion(), acceso.rol(),
				movimientoRepository.sumarPagosPendientes(acceso.relacion().getId()));
	}
}
