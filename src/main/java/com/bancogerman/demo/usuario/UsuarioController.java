package com.bancogerman.demo.usuario;

import com.bancogerman.demo.common.SecurityUtils;
import com.bancogerman.demo.usuario.dto.ActualizarUsuarioRequest;
import com.bancogerman.demo.usuario.dto.UsuarioResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

	private final UsuarioService usuarioService;

	@GetMapping("/me")
	public UsuarioResponse yo() {
		return usuarioService.obtener(SecurityUtils.usuarioActualId());
	}

	@PatchMapping("/me")
	public UsuarioResponse actualizar(@Valid @RequestBody ActualizarUsuarioRequest request) {
		return usuarioService.actualizar(SecurityUtils.usuarioActualId(), request);
	}
}
