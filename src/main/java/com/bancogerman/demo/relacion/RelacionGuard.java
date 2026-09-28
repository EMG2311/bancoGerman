package com.bancogerman.demo.relacion;

import java.util.Optional;

import com.bancogerman.demo.common.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Control de acceso por relación. Todo endpoint que opera sobre una relación pasa por acá.
 * <ul>
 *   <li>Si el usuario no es acreedor ni deudor → 404 (no revelamos que la relación existe).</li>
 *   <li>Si es parte pero del lado equivocado → 403, vía {@link RelacionAcceso#exigir}.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class RelacionGuard {

	private final RelacionRepository relacionRepository;

	@Transactional(propagation = Propagation.MANDATORY, readOnly = true)
	public RelacionAcceso acceder(Long relacionId, Long usuarioId) {
		return verificar(relacionRepository.findById(relacionId), usuarioId);
	}

	/** Igual que {@link #acceder} pero bloquea la fila hasta el fin de la transacción. */
	@Transactional(propagation = Propagation.MANDATORY)
	public RelacionAcceso accederParaModificar(Long relacionId, Long usuarioId) {
		return verificar(relacionRepository.findByIdParaModificar(relacionId), usuarioId);
	}

	private RelacionAcceso verificar(Optional<Relacion> relacion, Long usuarioId) {
		return relacion
				.flatMap(r -> r.rolDe(usuarioId).map(rol -> new RelacionAcceso(r, rol)))
				.orElseThrow(() -> ApiException.noEncontrado("Relación no encontrada"));
	}
}
