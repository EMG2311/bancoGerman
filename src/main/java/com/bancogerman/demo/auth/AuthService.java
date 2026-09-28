package com.bancogerman.demo.auth;

import com.bancogerman.demo.auth.dto.LoginRequest;
import com.bancogerman.demo.auth.dto.RegistroRequest;
import com.bancogerman.demo.auth.dto.TokenResponse;
import com.bancogerman.demo.common.ApiException;
import com.bancogerman.demo.usuario.Usuario;
import com.bancogerman.demo.usuario.UsuarioRepository;
import com.bancogerman.demo.usuario.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	@Transactional
	public TokenResponse registrar(RegistroRequest request) {
		String email = UsuarioService.normalizarEmail(request.email());
		if (usuarioRepository.existsByEmail(email)) {
			throw ApiException.conflicto("Ya existe un usuario con ese email");
		}
		String telefono = request.telefono() == null || request.telefono().isBlank() ? null : request.telefono().trim();
		Usuario usuario = usuarioRepository.save(new Usuario(request.nombre().trim(), email, telefono,
				passwordEncoder.encode(request.password())));
		return jwtService.emitir(usuario);
	}

	@Transactional(readOnly = true)
	public TokenResponse login(LoginRequest request) {
		Usuario usuario = usuarioRepository.findByEmail(UsuarioService.normalizarEmail(request.email()))
				.filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
				.orElseThrow(() -> ApiException.noAutorizado("Email o contraseña incorrectos"));
		return jwtService.emitir(usuario);
	}
}
