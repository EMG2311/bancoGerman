package com.bancogerman.demo.movimiento;

import com.bancogerman.demo.common.Pagina;
import com.bancogerman.demo.common.SecurityUtils;
import com.bancogerman.demo.movimiento.dto.CargoRequest;
import com.bancogerman.demo.movimiento.dto.MovimientoResponse;
import com.bancogerman.demo.movimiento.dto.PagoRequest;
import com.bancogerman.demo.movimiento.dto.SaldoResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class MovimientoController {

	private final MovimientoService movimientoService;

	@GetMapping("/relaciones/{relacionId}/movimientos")
	public Pagina<MovimientoResponse> listar(@PathVariable Long relacionId,
			@RequestParam(required = false) EstadoMovimiento estado,
			@RequestParam(defaultValue = "0") @Min(0) int pagina,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanio) {
		return movimientoService.listar(relacionId, SecurityUtils.usuarioActualId(), estado, pagina, tamanio);
	}

	@GetMapping("/relaciones/{relacionId}/saldo")
	public SaldoResponse saldo(@PathVariable Long relacionId) {
		return movimientoService.saldo(relacionId, SecurityUtils.usuarioActualId());
	}

	@PostMapping("/relaciones/{relacionId}/cargos")
	@ResponseStatus(HttpStatus.CREATED)
	public MovimientoResponse registrarCargo(@PathVariable Long relacionId, @Valid @RequestBody CargoRequest request) {
		return movimientoService.registrarCargo(relacionId, SecurityUtils.usuarioActualId(), request);
	}

	@PostMapping("/relaciones/{relacionId}/pagos")
	@ResponseStatus(HttpStatus.CREATED)
	public MovimientoResponse registrarPago(@PathVariable Long relacionId, @Valid @RequestBody PagoRequest request) {
		return movimientoService.registrarPago(relacionId, SecurityUtils.usuarioActualId(), request);
	}

	@PostMapping("/movimientos/{id}/confirmar")
	public MovimientoResponse confirmar(@PathVariable Long id) {
		return movimientoService.confirmar(id, SecurityUtils.usuarioActualId());
	}

	@PostMapping("/movimientos/{id}/rechazar")
	public MovimientoResponse rechazar(@PathVariable Long id) {
		return movimientoService.rechazar(id, SecurityUtils.usuarioActualId());
	}
}
