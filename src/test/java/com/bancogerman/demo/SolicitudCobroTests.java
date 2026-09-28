package com.bancogerman.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

class SolicitudCobroTests extends ApiTestBase {

	@Test
	void soloElAcreedorPideYNoMasDeLoQueLeDeben() throws Exception {
		Usr banco = registrar("Banco");
		Usr deudor = registrar("Deudor");
		Long id = relacionActiva(banco, deudor, false);
		cargo(banco, id, "500");

		post(deudor, "/api/relaciones/" + id + "/solicitudes", """
				{"monto":100}""").andExpect(status().isForbidden());
		post(banco, "/api/relaciones/" + id + "/solicitudes", """
				{"monto":600}""").andExpect(status().isUnprocessableContent());
	}

	@Test
	void crearSolicitudNotificaAlDeudorPorPush() throws Exception {
		Usr banco = registrar("Banco");
		Usr deudor = registrar("Deudor");
		Long id = relacionActiva(banco, deudor, false);
		post(deudor, "/api/dispositivos", """
				{"pushToken":"token-deudor-%s","plataforma":"IOS"}""".formatted(deudor.id()));
		cargo(banco, id, "500");

		Long solicitud = idDe(post(banco, "/api/relaciones/" + id + "/solicitudes", """
				{"monto":200,"descripcion":"lo del asado"}""").andExpect(status().isCreated())
				.andExpect(jsonPath("$.estado").value("PENDIENTE")));

		verify(pushSender, timeout(5000)).enviar(argThat(m -> m.token().equals("token-deudor-" + deudor.id())
				&& m.datos().get("tipo").equals("SOLICITUD_COBRO")
				&& m.datos().get("solicitudId").equals(solicitud.toString())
				&& m.cuerpo().contains("$200.00")));
	}

	@Test
	void pagarSolicitudSinConfirmacionLaCierraAlInstante() throws Exception {
		Usr banco = registrar("Banco");
		Usr deudor = registrar("Deudor");
		Long id = relacionActiva(banco, deudor, false);
		cargo(banco, id, "500");
		Long solicitud = idDe(post(banco, "/api/relaciones/" + id + "/solicitudes", """
				{"monto":200}"""));

		post(banco, "/api/solicitudes/" + solicitud + "/pagar", """
				{"medioPago":"TRANSFERENCIA"}""").andExpect(status().isForbidden());
		post(deudor, "/api/solicitudes/" + solicitud + "/pagar", """
				{"medioPago":"TRANSFERENCIA"}""").andExpect(status().isCreated())
				.andExpect(jsonPath("$.estado").value("CONFIRMADO"))
				.andExpect(jsonPath("$.solicitudId").value(solicitud));

		assertThat(saldo(banco, id)).isEqualByComparingTo("300");
		get(banco, "/api/relaciones/" + id + "/solicitudes").andExpect(jsonPath("$[0].estado").value("PAGADA"));
		post(deudor, "/api/solicitudes/" + solicitud + "/pagar", """
				{"medioPago":"TRANSFERENCIA"}""").andExpect(status().isConflict());
	}

	@Test
	void pagarSolicitudConConfirmacionEsperaAlAcreedor() throws Exception {
		Usr banco = registrar("Banco");
		Usr deudor = registrar("Deudor");
		Long id = relacionActiva(banco, deudor, true);
		cargo(banco, id, "500");
		Long solicitud = idDe(post(banco, "/api/relaciones/" + id + "/solicitudes", """
				{"monto":200}"""));

		Long pago = idDe(post(deudor, "/api/solicitudes/" + solicitud + "/pagar", """
				{"medioPago":"EFECTIVO"}""").andExpect(jsonPath("$.estado").value("PENDIENTE")));
		get(banco, "/api/relaciones/" + id + "/solicitudes").andExpect(jsonPath("$[0].estado").value("PENDIENTE"));

		// Mientras hay un pago en curso no se puede pagar de nuevo ni rechazar
		post(deudor, "/api/solicitudes/" + solicitud + "/pagar", """
				{"medioPago":"EFECTIVO"}""").andExpect(status().isConflict());
		post(deudor, "/api/solicitudes/" + solicitud + "/rechazar").andExpect(status().isConflict());

		post(banco, "/api/movimientos/" + pago + "/confirmar").andExpect(status().isOk());
		get(banco, "/api/relaciones/" + id + "/solicitudes").andExpect(jsonPath("$[0].estado").value("PAGADA"));
		assertThat(saldo(banco, id)).isEqualByComparingTo("300");
	}

	@Test
	void siElAcreedorRechazaElPagoLaSolicitudSigueAbierta() throws Exception {
		Usr banco = registrar("Banco");
		Usr deudor = registrar("Deudor");
		Long id = relacionActiva(banco, deudor, true);
		cargo(banco, id, "500");
		Long solicitud = idDe(post(banco, "/api/relaciones/" + id + "/solicitudes", """
				{"monto":200}"""));
		Long pago = idDe(post(deudor, "/api/solicitudes/" + solicitud + "/pagar", """
				{"medioPago":"TRANSFERENCIA"}"""));

		post(banco, "/api/movimientos/" + pago + "/rechazar").andExpect(status().isOk());

		get(banco, "/api/relaciones/" + id + "/solicitudes").andExpect(jsonPath("$[0].estado").value("PENDIENTE"));
		post(deudor, "/api/solicitudes/" + solicitud + "/pagar", """
				{"medioPago":"TRANSFERENCIA"}""").andExpect(status().isCreated());
	}

	@Test
	void elDeudorRechazaLaSolicitud() throws Exception {
		Usr banco = registrar("Banco");
		Usr deudor = registrar("Deudor");
		Long id = relacionActiva(banco, deudor, false);
		cargo(banco, id, "500");
		Long solicitud = idDe(post(banco, "/api/relaciones/" + id + "/solicitudes", """
				{"monto":200}"""));

		post(banco, "/api/solicitudes/" + solicitud + "/rechazar").andExpect(status().isForbidden());
		post(deudor, "/api/solicitudes/" + solicitud + "/rechazar").andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("RECHAZADA"));

		assertThat(saldo(banco, id)).isEqualByComparingTo("500");
		get(banco, "/api/notificaciones").andExpect(jsonPath("$.contenido[0].tipo").value("SOLICITUD_RECHAZADA"));
	}
}
