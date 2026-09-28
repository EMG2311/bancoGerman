package com.bancogerman.demo.notificacion;

import java.util.LinkedHashMap;
import java.util.Map;

import com.bancogerman.demo.common.ApiException;
import com.bancogerman.demo.common.Pagina;
import com.bancogerman.demo.notificacion.dto.NotificacionResponse;
import com.bancogerman.demo.notificacion.push.PushPendiente;
import com.bancogerman.demo.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class NotificacionService {

	private final NotificacionRepository notificacionRepository;
	private final UsuarioRepository usuarioRepository;
	private final JsonMapper jsonMapper;

	/**
	 * Guarda la notificación en el historial y arma el push para mandar. Corre en su propia
	 * transacción porque se invoca después del commit de la operación de negocio.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public PushPendiente registrar(NotificacionEvento evento) {
		Notificacion notificacion = notificacionRepository.save(new Notificacion(
				usuarioRepository.getReferenceById(evento.destinatarioId()), evento.tipo(),
				jsonMapper.writeValueAsString(evento.datos())));

		Map<String, String> datosPush = new LinkedHashMap<>();
		datosPush.put("notificacionId", notificacion.getId().toString());
		datosPush.put("tipo", evento.tipo().name());
		evento.datos().forEach((clave, valor) -> datosPush.put(clave, String.valueOf(valor)));
		return new PushPendiente(evento.destinatarioId(), evento.tipo().titulo(), evento.tipo().cuerpo(evento.datos()),
				datosPush);
	}

	@Transactional(readOnly = true)
	public Pagina<NotificacionResponse> listar(Long usuarioId, boolean soloNoLeidas, int pagina, int tamanio) {
		PageRequest pageable = PageRequest.of(pagina, tamanio);
		var page = soloNoLeidas
				? notificacionRepository.findByUsuarioIdAndLeidaFalseOrderByFechaDescIdDesc(usuarioId, pageable)
				: notificacionRepository.findByUsuarioIdOrderByFechaDescIdDesc(usuarioId, pageable);
		return Pagina.de(page, NotificacionResponse::de);
	}

	@Transactional(readOnly = true)
	public long contarNoLeidas(Long usuarioId) {
		return notificacionRepository.countByUsuarioIdAndLeidaFalse(usuarioId);
	}

	@Transactional
	public NotificacionResponse marcarLeida(Long usuarioId, Long notificacionId) {
		Notificacion notificacion = notificacionRepository.findByIdAndUsuarioId(notificacionId, usuarioId)
				.orElseThrow(() -> ApiException.noEncontrado("Notificación no encontrada"));
		notificacion.marcarLeida();
		return NotificacionResponse.de(notificacion);
	}

	@Transactional
	public int marcarTodasLeidas(Long usuarioId) {
		return notificacionRepository.marcarTodasLeidas(usuarioId);
	}
}
