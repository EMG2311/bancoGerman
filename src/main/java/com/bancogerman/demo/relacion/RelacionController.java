package com.bancogerman.demo.relacion;

import java.util.List;

import com.bancogerman.demo.common.SecurityUtils;
import com.bancogerman.demo.relacion.dto.CambiarConfirmacionRequest;
import com.bancogerman.demo.relacion.dto.InvitarRequest;
import com.bancogerman.demo.relacion.dto.RelacionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/relaciones")
@RequiredArgsConstructor
public class RelacionController {

	private final RelacionService relacionService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public RelacionResponse invitar(@Valid @RequestBody InvitarRequest request) {
		return relacionService.invitar(SecurityUtils.usuarioActualId(), request);
	}

	@GetMapping
	public List<RelacionResponse> listar(@RequestParam(required = false) Rol rol,
			@RequestParam(required = false) EstadoRelacion estado) {
		return relacionService.listar(SecurityUtils.usuarioActualId(), rol, estado);
	}

	@GetMapping("/{id}")
	public RelacionResponse obtener(@PathVariable Long id) {
		return relacionService.obtener(id, SecurityUtils.usuarioActualId());
	}

	@PostMapping("/{id}/aceptar")
	public RelacionResponse aceptar(@PathVariable Long id) {
		return relacionService.aceptar(id, SecurityUtils.usuarioActualId());
	}

	@PostMapping("/{id}/rechazar")
	public RelacionResponse rechazar(@PathVariable Long id) {
		return relacionService.rechazar(id, SecurityUtils.usuarioActualId());
	}

	@PatchMapping("/{id}/confirmacion")
	public RelacionResponse cambiarConfirmacion(@PathVariable Long id,
			@Valid @RequestBody CambiarConfirmacionRequest request) {
		return relacionService.cambiarConfirmacion(id, SecurityUtils.usuarioActualId(),
				request.requiereConfirmacion());
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void eliminar(@PathVariable Long id) {
		relacionService.eliminar(id, SecurityUtils.usuarioActualId());
	}
}
