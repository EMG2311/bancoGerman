package com.bancogerman.demo.usuario;

import com.bancogerman.demo.common.ApiException;
import com.bancogerman.demo.usuario.dto.ActualizarUsuarioRequest;
import com.bancogerman.demo.usuario.dto.UsuarioResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

	private final UsuarioRepository usuarioRepository;

	@Transactional(readOnly = true)
	public UsuarioResponse obtener(Long usuarioId) {
		return UsuarioResponse.de(buscar(usuarioId));
	}

	@Transactional
	public UsuarioResponse actualizar(Long usuarioId, ActualizarUsuarioRequest request) {
		Usuario usuario = buscar(usuarioId);
		if (request.nombre() != null) {
			usuario.setNombre(request.nombre().trim());
		}
		if (request.telefono() != null) {
			usuario.setTelefono(request.telefono().isBlank() ? null : request.telefono().trim());
		}
		return UsuarioResponse.de(usuario);
	}

	private Usuario buscar(Long usuarioId) {
		return usuarioRepository.findById(usuarioId)
				.orElseThrow(() -> ApiException.noEncontrado("Usuario no encontrado"));
	}

	/** Emails siempre en minúscula y sin espacios, para que el UNIQUE no dependa de cómo se tipeó. */
	public static String normalizarEmail(String email) {
		return email.trim().toLowerCase();
	}
}
