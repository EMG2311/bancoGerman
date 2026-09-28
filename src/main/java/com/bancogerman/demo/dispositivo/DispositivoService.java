package com.bancogerman.demo.dispositivo;

import java.util.List;

import com.bancogerman.demo.common.ApiException;
import com.bancogerman.demo.dispositivo.dto.DispositivoResponse;
import com.bancogerman.demo.dispositivo.dto.RegistrarDispositivoRequest;
import com.bancogerman.demo.usuario.Usuario;
import com.bancogerman.demo.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DispositivoService {

	private final DispositivoRepository dispositivoRepository;
	private final UsuarioRepository usuarioRepository;

	/** Idempotente: registrar el mismo token dos veces no duplica el dispositivo. */
	@Transactional
	public DispositivoResponse registrar(Long usuarioId, RegistrarDispositivoRequest request) {
		Usuario usuario = usuarioRepository.getReferenceById(usuarioId);
		String token = request.pushToken().trim();
		Dispositivo dispositivo = dispositivoRepository.findByPushToken(token)
				.map(existente -> {
					existente.asignarA(usuario, request.plataforma());
					return existente;
				})
				.orElseGet(() -> dispositivoRepository.save(new Dispositivo(usuario, token, request.plataforma())));
		return DispositivoResponse.de(dispositivo);
	}

	@Transactional(readOnly = true)
	public List<DispositivoResponse> listar(Long usuarioId) {
		return dispositivoRepository.findByUsuarioId(usuarioId).stream().map(DispositivoResponse::de).toList();
	}

	@Transactional
	public void eliminar(Long usuarioId, Long dispositivoId) {
		Dispositivo dispositivo = dispositivoRepository.findByIdAndUsuarioId(dispositivoId, usuarioId)
				.orElseThrow(() -> ApiException.noEncontrado("Dispositivo no encontrado"));
		dispositivoRepository.delete(dispositivo);
	}
}
