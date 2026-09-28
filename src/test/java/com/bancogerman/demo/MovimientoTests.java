package com.bancogerman.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.bancogerman.demo.common.ApiException;
import com.bancogerman.demo.movimiento.MedioPago;
import com.bancogerman.demo.movimiento.MovimientoService;
import com.bancogerman.demo.movimiento.dto.PagoRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class MovimientoTests extends ApiTestBase {

	@Autowired
	MovimientoService movimientoService;

	@Test
	void sinConfirmacionElPagoBajaElSaldoAlInstante() throws Exception {
		Usr banco = registrar("Banco");
		Usr deudor = registrar("Deudor");
		Long id = relacionActiva(banco, deudor, false);

		cargo(banco, id, "1000").andExpect(status().isCreated()).andExpect(jsonPath("$.estado").value("CONFIRMADO"));
		pago(deudor, id, "400").andExpect(status().isCreated())
				.andExpect(jsonPath("$.estado").value("CONFIRMADO"))
				.andExpect(jsonPath("$.requirioConfirmacion").value(false))
				.andExpect(jsonPath("$.medioPago").value("TRANSFERENCIA"));

		assertThat(saldo(banco, id)).isEqualByComparingTo("600");
		get(banco, "/api/notificaciones").andExpect(jsonPath("$.contenido[0].tipo").value("PAGO_REGISTRADO"));
	}

	@Test
	void unPagoMayorALaDeudaSeRechaza() throws Exception {
		Usr banco = registrar("Banco");
		Usr deudor = registrar("Deudor");
		Long id = relacionActiva(banco, deudor, false);
		cargo(banco, id, "1000");

		pago(deudor, id, "1500").andExpect(status().isUnprocessableContent());
		pago(deudor, id, "1000.01").andExpect(status().isUnprocessableContent());
		assertThat(saldo(banco, id)).isEqualByComparingTo("1000");

		pago(deudor, id, "1000").andExpect(status().isCreated());
		assertThat(saldo(banco, id)).isEqualByComparingTo("0");
		pago(deudor, id, "0.01").andExpect(status().isUnprocessableContent());
	}

	@Test
	void validacionesDeFormato() throws Exception {
		Usr banco = registrar("Banco");
		Usr deudor = registrar("Deudor");
		Long id = relacionActiva(banco, deudor, false);
		cargo(banco, id, "1000");

		post(deudor, "/api/relaciones/" + id + "/pagos", """
				{"monto":100}""").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores.medioPago").exists());
		post(deudor, "/api/relaciones/" + id + "/pagos", """
				{"monto":100,"medioPago":"CHEQUE"}""").andExpect(status().isBadRequest());
		pago(deudor, id, "-5").andExpect(status().isBadRequest());
		pago(deudor, id, "0").andExpect(status().isBadRequest());
		pago(deudor, id, "10.001").andExpect(status().isBadRequest());
	}

	@Test
	void permisosPorLado() throws Exception {
		Usr banco = registrar("Banco");
		Usr deudor = registrar("Deudor");
		Long id = relacionActiva(banco, deudor, true);
		cargo(deudor, id, "100").andExpect(status().isForbidden());
		cargo(banco, id, "100");
		Long pagoId = idDe(pago(deudor, id, "50"));

		post(deudor, "/api/movimientos/" + pagoId + "/confirmar").andExpect(status().isForbidden());
		post(deudor, "/api/movimientos/" + pagoId + "/rechazar").andExpect(status().isForbidden());
	}

	@Test
	void conConfirmacionElPagoQuedaPendienteHastaQueElAcreedorConfirma() throws Exception {
		Usr banco = registrar("Banco");
		Usr deudor = registrar("Deudor");
		Long id = relacionActiva(banco, deudor, true);
		post(banco, "/api/dispositivos", """
				{"pushToken":"token-banco-%s","plataforma":"ANDROID"}""".formatted(banco.id())).andExpect(status().isOk());
		cargo(banco, id, "1000");

		Long pagoId = idDe(pago(deudor, id, "300").andExpect(jsonPath("$.estado").value("PENDIENTE"))
				.andExpect(jsonPath("$.requirioConfirmacion").value(true)));

		// El saldo no se movió, pero lo pendiente ya no se puede volver a pagar
		get(banco, "/api/relaciones/" + id)
				.andExpect(jsonPath("$.saldo").value(1000.0))
				.andExpect(jsonPath("$.pendienteConfirmacion").value(300.0))
				.andExpect(jsonPath("$.disponibleParaPagar").value(700.0));
		pago(deudor, id, "800").andExpect(status().isUnprocessableContent());

		// Push al acreedor
		verify(pushSender, timeout(5000)).enviar(argThat(m -> m.token().equals("token-banco-" + banco.id())
				&& m.datos().get("tipo").equals("PAGO_PENDIENTE")
				&& m.datos().get("movimientoId").equals(pagoId.toString())));
		get(banco, "/api/notificaciones").andExpect(jsonPath("$.contenido[0].tipo").value("PAGO_PENDIENTE"));

		post(banco, "/api/movimientos/" + pagoId + "/confirmar").andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("CONFIRMADO"));
		assertThat(saldo(banco, id)).isEqualByComparingTo("700");
		get(deudor, "/api/notificaciones").andExpect(jsonPath("$.contenido[0].tipo").value("PAGO_CONFIRMADO"));

		post(banco, "/api/movimientos/" + pagoId + "/confirmar").andExpect(status().isConflict());
		post(banco, "/api/movimientos/" + pagoId + "/rechazar").andExpect(status().isConflict());
	}

	@Test
	void rechazarUnPagoNoMueveElSaldo() throws Exception {
		Usr banco = registrar("Banco");
		Usr deudor = registrar("Deudor");
		Long id = relacionActiva(banco, deudor, true);
		cargo(banco, id, "1000");
		Long pagoId = idDe(pago(deudor, id, "1000"));

		post(banco, "/api/movimientos/" + pagoId + "/rechazar").andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("RECHAZADO"));

		assertThat(saldo(banco, id)).isEqualByComparingTo("1000");
		get(deudor, "/api/notificaciones").andExpect(jsonPath("$.contenido[0].tipo").value("PAGO_RECHAZADO"));
		// Lo rechazado deja de reservar saldo
		pago(deudor, id, "1000").andExpect(status().isCreated());
	}

	@Test
	void losCargosNoSeConfirmanNiRechazan() throws Exception {
		Usr banco = registrar("Banco");
		Usr deudor = registrar("Deudor");
		Long id = relacionActiva(banco, deudor, true);
		Long cargoId = idDe(cargo(banco, id, "1000").andExpect(jsonPath("$.estado").value("CONFIRMADO")));

		post(banco, "/api/movimientos/" + cargoId + "/rechazar").andExpect(status().isConflict());
	}

	@Test
	void elFlagNoEsRetroactivo() throws Exception {
		Usr banco = registrar("Banco");
		Usr deudor = registrar("Deudor");
		Long id = relacionActiva(banco, deudor, true);
		cargo(banco, id, "1000");
		Long pendiente = idDe(pago(deudor, id, "100"));

		patch(banco, "/api/relaciones/" + id + "/confirmacion", """
				{"requiereConfirmacion":false}""").andExpect(status().isOk());

		// El pago viejo sigue esperando confirmación y el saldo no se tocó
		get(banco, "/api/relaciones/" + id + "/movimientos?estado=PENDIENTE")
				.andExpect(jsonPath("$.contenido[0].id").value(pendiente))
				.andExpect(jsonPath("$.contenido[0].requirioConfirmacion").value(true));
		assertThat(saldo(banco, id)).isEqualByComparingTo("1000");

		// Los nuevos nacen confirmados
		pago(deudor, id, "200").andExpect(jsonPath("$.estado").value("CONFIRMADO"));
		assertThat(saldo(banco, id)).isEqualByComparingTo("800");

		post(banco, "/api/movimientos/" + pendiente + "/confirmar").andExpect(status().isOk());
		assertThat(saldo(banco, id)).isEqualByComparingTo("700");
	}

	@Test
	void elAcreedorRegistraUnPagoEnEfectivoQueNaceConfirmado() throws Exception {
		Usr banco = registrar("Banco");
		Usr deudor = registrar("Deudor");
		Long id = relacionActiva(banco, deudor, true);
		cargo(banco, id, "1000");

		post(banco, "/api/relaciones/" + id + "/pagos", """
				{"monto":250,"medioPago":"EFECTIVO","descripcion":"me lo dio en mano"}""")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.estado").value("CONFIRMADO"))
				.andExpect(jsonPath("$.medioPago").value("EFECTIVO"))
				.andExpect(jsonPath("$.creadoPorId").value(banco.id()));

		assertThat(saldo(deudor, id)).isEqualByComparingTo("750");
		get(deudor, "/api/notificaciones").andExpect(jsonPath("$.contenido[0].tipo").value("PAGO_REGISTRADO"));
	}

	@Test
	void elSaldoCacheadoCoincideConElLedger() throws Exception {
		Usr banco = registrar("Banco");
		Usr deudor = registrar("Deudor");
		Long id = relacionActiva(banco, deudor, true);
		cargo(banco, id, "1000");
		cargo(banco, id, "234.56");
		post(banco, "/api/movimientos/" + idDe(pago(deudor, id, "100.10")) + "/confirmar");
		post(banco, "/api/movimientos/" + idDe(pago(deudor, id, "50")) + "/rechazar");
		pago(deudor, id, "10");
		post(banco, "/api/relaciones/" + id + "/pagos", """
				{"monto":24.46,"medioPago":"EFECTIVO"}""");

		get(banco, "/api/relaciones/" + id + "/saldo")
				.andExpect(jsonPath("$.saldo").value(1110.0))
				.andExpect(jsonPath("$.saldoSegunLedger").value(1110.0))
				.andExpect(jsonPath("$.pendienteConfirmacion").value(10.0))
				.andExpect(jsonPath("$.consistente").value(true));
	}

	@Test
	void pagosConcurrentesNuncaDejanElSaldoNegativo() throws Exception {
		Usr banco = registrar("Banco");
		Usr deudor = registrar("Deudor");
		Long id = relacionActiva(banco, deudor, false);
		cargo(banco, id, "1000");

		int intentos = 10;
		ExecutorService pool = Executors.newFixedThreadPool(intentos);
		List<Callable<Boolean>> tareas = new ArrayList<>();
		for (int i = 0; i < intentos; i++) {
			tareas.add(() -> {
				try {
					movimientoService.registrarPago(id, deudor.id(),
							new PagoRequest(new BigDecimal("200"), MedioPago.TRANSFERENCIA, null));
					return true;
				} catch (ApiException e) {
					return false;
				}
			});
		}
		int exitosos = 0;
		for (Future<Boolean> f : pool.invokeAll(tareas)) {
			if (f.get()) {
				exitosos++;
			}
		}
		pool.shutdown();

		assertThat(exitosos).isEqualTo(5);
		assertThat(saldo(banco, id)).isEqualByComparingTo("0");
		get(banco, "/api/relaciones/" + id + "/saldo").andExpect(jsonPath("$.consistente").value(true));
	}
}
