package com.bancogerman.demo.dispositivo;

import java.util.List;

import com.bancogerman.demo.common.SecurityUtils;
import com.bancogerman.demo.dispositivo.dto.DispositivoResponse;
import com.bancogerman.demo.dispositivo.dto.RegistrarDispositivoRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dispositivos")
@RequiredArgsConstructor
public class DispositivoController {

	private final DispositivoService dispositivoService;

	@PostMapping
	public DispositivoResponse registrar(@Valid @RequestBody RegistrarDispositivoRequest request) {
		return dispositivoService.registrar(SecurityUtils.usuarioActualId(), request);
	}

	@GetMapping
	public List<DispositivoResponse> listar() {
		return dispositivoService.listar(SecurityUtils.usuarioActualId());
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void eliminar(@PathVariable Long id) {
		dispositivoService.eliminar(SecurityUtils.usuarioActualId(), id);
	}
}
