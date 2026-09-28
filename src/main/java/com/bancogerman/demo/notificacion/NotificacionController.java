package com.bancogerman.demo.notificacion;

import java.util.Map;

import com.bancogerman.demo.common.Pagina;
import com.bancogerman.demo.common.SecurityUtils;
import com.bancogerman.demo.notificacion.dto.NotificacionResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

	private final NotificacionService notificacionService;

	@GetMapping
	public Pagina<NotificacionResponse> listar(@RequestParam(defaultValue = "false") boolean soloNoLeidas,
			@RequestParam(defaultValue = "0") @Min(0) int pagina,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanio) {
		return notificacionService.listar(SecurityUtils.usuarioActualId(), soloNoLeidas, pagina, tamanio);
	}

	@GetMapping("/no-leidas/cantidad")
	public Map<String, Long> cantidadNoLeidas() {
		return Map.of("cantidad", notificacionService.contarNoLeidas(SecurityUtils.usuarioActualId()));
	}

	@PatchMapping("/{id}/leida")
	public NotificacionResponse marcarLeida(@PathVariable Long id) {
		return notificacionService.marcarLeida(SecurityUtils.usuarioActualId(), id);
	}

	@PostMapping("/leer-todas")
	public Map<String, Integer> marcarTodasLeidas() {
		return Map.of("actualizadas", notificacionService.marcarTodasLeidas(SecurityUtils.usuarioActualId()));
	}
}
