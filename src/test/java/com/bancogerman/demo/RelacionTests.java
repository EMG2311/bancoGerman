package com.bancogerman.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;

class RelacionTests extends ApiTestBase {

	@Test
	void laRelacionNaceInvitadaYElDeudorTieneQueAceptar() throws Exception {
		Usr banco = registrar("Banco");
		Usr deudor = registrar("Deudor");
		Long id = invitar(banco, deudor, false);

		get(banco, "/api/relaciones/" + id).andExpect(jsonPath("$.estado").value("INVITADA"))
				.andExpect(jsonPath("$.miRol").value("ACREEDOR"));
		cargo(banco, id, "100").andExpect(status().isConflict());

		// Al deudor le llegó la invitación
		get(deudor, "/api/notificaciones").andExpect(jsonPath("$.contenido[0].tipo").value("INVITACION_RELACION"))
				.andExpect(jsonPath("$.contenido[0].payload.relacionId").value(id));

		post(banco, "/api/relaciones/" + id + "/aceptar").andExpect(status().isForbidden());
		post(deudor, "/api/relaciones/" + id + "/aceptar").andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("ACTIVA"))
				.andExpect(jsonPath("$.miRol").value("DEUDOR"));
		post(deudor, "/api/relaciones/" + id + "/aceptar").andExpect(status().isConflict());

		cargo(banco, id, "100").andExpect(status().isCreated());
	}

	@Test
	void rechazarInvitacionPermiteVolverAInvitar() throws Exception {
		Usr banco = registrar("Banco");
		Usr deudor = registrar("Deudor");
		Long id = invitar(banco, deudor, false);

		post(deudor, "/api/relaciones/" + id + "/rechazar").andExpect(jsonPath("$.estado").value("RECHAZADA"));
		get(banco, "/api/notificaciones").andExpect(jsonPath("$.contenido[0].tipo").value("RELACION_RECHAZADA"));

		invitar(banco, deudor, false);
	}

	@Test
	void validacionesAlInvitar() throws Exception {
		Usr banco = registrar("Banco");
		Usr deudor = registrar("Deudor");
		invitar(banco, deudor, false);

		post(banco, "/api/relaciones", """
				{"emailDeudor":"%s"}""".formatted(deudor.email())).andExpect(status().isConflict());
		post(banco, "/api/relaciones", """
				{"emailDeudor":"%s"}""".formatted(banco.email())).andExpect(status().isUnprocessableContent());
		post(banco, "/api/relaciones", """
				{"emailDeudor":"nadie-registrado@test.com"}""").andExpect(status().isNotFound());
	}

	@Test
	void deudasMutuasSonDosRelacionesSeparadas() throws Exception {
		Usr ana = registrar("Ana");
		Usr beto = registrar("Beto");
		Long anaLePrestaABeto = relacionActiva(ana, beto, false);
		Long betoLePrestaAAna = relacionActiva(beto, ana, false);
		cargo(ana, anaLePrestaABeto, "1000").andExpect(status().isCreated());
		cargo(beto, betoLePrestaAAna, "300").andExpect(status().isCreated());

		assertThat(saldo(ana, anaLePrestaABeto)).isEqualByComparingTo("1000");
		assertThat(saldo(ana, betoLePrestaAAna)).isEqualByComparingTo("300");

		List<Integer> comoAcreedor = leer(get(ana, "/api/relaciones?rol=ACREEDOR"), "$[*].id");
		List<Integer> comoDeudora = leer(get(ana, "/api/relaciones?rol=DEUDOR"), "$[*].id");
		assertThat(comoAcreedor).containsExactly(anaLePrestaABeto.intValue());
		assertThat(comoDeudora).containsExactly(betoLePrestaAAna.intValue());
		assertThat((List<?>) leer(get(ana, "/api/relaciones"), "$")).hasSize(2);
	}

	@Test
	void unTerceroNoVeLaRelacion() throws Exception {
		Usr banco = registrar("Banco");
		Usr deudor = registrar("Deudor");
		Usr intruso = registrar("Intruso");
		Long id = relacionActiva(banco, deudor, false);
		Long pagoId = idDe(cargo(banco, id, "100"));

		get(intruso, "/api/relaciones/" + id).andExpect(status().isNotFound());
		get(intruso, "/api/relaciones/" + id + "/movimientos").andExpect(status().isNotFound());
		cargo(intruso, id, "100").andExpect(status().isNotFound());
		pago(intruso, id, "10").andExpect(status().isNotFound());
		post(intruso, "/api/movimientos/" + pagoId + "/confirmar").andExpect(status().isNotFound());
		patch(intruso, "/api/relaciones/" + id + "/confirmacion", """
				{"requiereConfirmacion":true}""").andExpect(status().isNotFound());
		delete(intruso, "/api/relaciones/" + id).andExpect(status().isNotFound());
	}

	@Test
	void soloElAcreedorCambiaElFlagDeConfirmacion() throws Exception {
		Usr banco = registrar("Banco");
		Usr deudor = registrar("Deudor");
		Long id = relacionActiva(banco, deudor, false);

		patch(deudor, "/api/relaciones/" + id + "/confirmacion", """
				{"requiereConfirmacion":true}""").andExpect(status().isForbidden());
		patch(banco, "/api/relaciones/" + id + "/confirmacion", """
				{"requiereConfirmacion":true}""").andExpect(status().isOk())
				.andExpect(jsonPath("$.requiereConfirmacion").value(true));
	}

	@Test
	void elDeudorNoPuedeEliminarSiDebe() throws Exception {
		Usr banco = registrar("Banco");
		Usr deudor = registrar("Deudor");
		Long id = relacionActiva(banco, deudor, false);
		cargo(banco, id, "500");

		delete(deudor, "/api/relaciones/" + id).andExpect(status().isUnprocessableContent());

		pago(deudor, id, "500").andExpect(status().isCreated());
		delete(deudor, "/api/relaciones/" + id).andExpect(status().isNoContent());
		get(banco, "/api/notificaciones").andExpect(jsonPath("$.contenido[0].tipo").value("RELACION_ELIMINADA"));
	}

	@Test
	void elAcreedorEliminaYSeRechazaLoPendiente() throws Exception {
		Usr banco = registrar("Banco");
		Usr deudor = registrar("Deudor");
		Long id = relacionActiva(banco, deudor, true);
		cargo(banco, id, "1000");
		Long pagoPendiente = idDe(pago(deudor, id, "200"));
		Long solicitud = idDe(post(banco, "/api/relaciones/" + id + "/solicitudes", """
				{"monto":300}"""));

		delete(banco, "/api/relaciones/" + id).andExpect(status().isNoContent());

		get(banco, "/api/relaciones/" + id).andExpect(jsonPath("$.estado").value("ELIMINADA"));
		get(banco, "/api/relaciones/" + id + "/movimientos?estado=RECHAZADO")
				.andExpect(jsonPath("$.contenido[0].id").value(pagoPendiente));
		get(banco, "/api/relaciones/" + id + "/solicitudes")
				.andExpect(jsonPath("$[0].id").value(solicitud))
				.andExpect(jsonPath("$[0].estado").value("RECHAZADA"));
		cargo(banco, id, "10").andExpect(status().isConflict());
		delete(banco, "/api/relaciones/" + id).andExpect(status().isConflict());

		// No aparece en el listado por defecto, sí filtrando por estado
		assertThat((List<?>) leer(get(banco, "/api/relaciones"), "$")).isEmpty();
		assertThat((List<?>) leer(get(banco, "/api/relaciones?estado=ELIMINADA"), "$")).hasSize(1);

		// Se puede volver a empezar de cero
		invitar(banco, deudor, false);
	}
}
