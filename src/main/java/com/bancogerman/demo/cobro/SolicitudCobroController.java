package com.bancogerman.demo.cobro;

import java.util.List;

import com.bancogerman.demo.cobro.dto.CrearSolicitudRequest;
import com.bancogerman.demo.cobro.dto.PagarSolicitudRequest;
import com.bancogerman.demo.cobro.dto.SolicitudResponse;
import com.bancogerman.demo.common.SecurityUtils;
import com.bancogerman.demo.movimiento.dto.MovimientoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SolicitudCobroController {

	private final SolicitudCobroService solicitudService;

	@PostMapping("/relaciones/{relacionId}/solicitudes")
	@ResponseStatus(HttpStatus.CREATED)
	public SolicitudResponse crear(@PathVariable Long relacionId, @Valid @RequestBody CrearSolicitudRequest request) {
		return solicitudService.crear(relacionId, SecurityUtils.usuarioActualId(), request);
	}

	@GetMapping("/relaciones/{relacionId}/solicitudes")
	public List<SolicitudResponse> listar(@PathVariable Long relacionId,
			@RequestParam(required = false) EstadoSolicitud estado) {
		return solicitudService.listar(relacionId, SecurityUtils.usuarioActualId(), estado);
	}

	@PostMapping("/solicitudes/{id}/pagar")
	@ResponseStatus(HttpStatus.CREATED)
	public MovimientoResponse pagar(@PathVariable Long id, @Valid @RequestBody PagarSolicitudRequest request) {
		return solicitudService.pagar(id, SecurityUtils.usuarioActualId(), request);
	}

	@PostMapping("/solicitudes/{id}/rechazar")
	public SolicitudResponse rechazar(@PathVariable Long id) {
		return solicitudService.rechazar(id, SecurityUtils.usuarioActualId());
	}
}
