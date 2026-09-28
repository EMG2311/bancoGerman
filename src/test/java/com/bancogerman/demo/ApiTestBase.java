package com.bancogerman.demo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.Callable;

import com.bancogerman.demo.notificacion.push.PushSender;
import com.bancogerman.demo.notificacion.push.ResultadoPush;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Tests de integración end-to-end contra MySQL (base deudas_test). No son @Transactional a propósito:
 * las notificaciones se procesan después del commit, igual que en producción. Cada test crea sus
 * propios usuarios con emails únicos, así no hace falta limpiar la base.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class ApiTestBase {

	@Autowired
	protected MockMvc mvc;

	@MockitoBean
	protected PushSender pushSender;

	protected record Usr(Long id, String token, String email, String nombre) {
	}

	@BeforeEach
	void pushOkPorDefecto() {
		when(pushSender.enviar(any())).thenReturn(ResultadoPush.ENVIADO);
	}

	protected Usr registrar(String nombre) throws Exception {
		String email = nombre.toLowerCase() + "-" + UUID.randomUUID() + "@test.com";
		String body = mvc.perform(MockMvcRequestBuilders.post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"nombre":"%s","email":"%s","password":"secreto123"}""".formatted(nombre, email)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return new Usr(((Number) JsonPath.read(body, "$.usuario.id")).longValue(), JsonPath.read(body, "$.token"),
				email, nombre);
	}

	protected ResultActions post(Usr u, String url, String json) throws Exception {
		return mvc.perform(conAuth(MockMvcRequestBuilders.post(url), u)
				.contentType(MediaType.APPLICATION_JSON).content(json == null ? "" : json));
	}

	protected ResultActions post(Usr u, String url) throws Exception {
		return post(u, url, null);
	}

	protected ResultActions patch(Usr u, String url, String json) throws Exception {
		return mvc.perform(conAuth(MockMvcRequestBuilders.patch(url), u)
				.contentType(MediaType.APPLICATION_JSON).content(json));
	}

	protected ResultActions get(Usr u, String url) throws Exception {
		return mvc.perform(conAuth(MockMvcRequestBuilders.get(url), u));
	}

	protected ResultActions delete(Usr u, String url) throws Exception {
		return mvc.perform(conAuth(MockMvcRequestBuilders.delete(url), u));
	}

	private MockHttpServletRequestBuilder conAuth(MockHttpServletRequestBuilder builder, Usr u) {
		return builder.header("Authorization", "Bearer " + u.token());
	}

	/** Invitación + aceptación: devuelve el id de una relación ACTIVA acreedor → deudor. */
	protected Long relacionActiva(Usr acreedor, Usr deudor, boolean requiereConfirmacion) throws Exception {
		Long id = invitar(acreedor, deudor, requiereConfirmacion);
		post(deudor, "/api/relaciones/" + id + "/aceptar").andExpect(status().isOk());
		return id;
	}

	protected Long invitar(Usr acreedor, Usr deudor, boolean requiereConfirmacion) throws Exception {
		return idDe(post(acreedor, "/api/relaciones", """
				{"emailDeudor":"%s","requiereConfirmacion":%s}""".formatted(deudor.email(), requiereConfirmacion))
				.andExpect(status().isCreated()));
	}

	protected ResultActions cargo(Usr u, Long relacionId, String monto) throws Exception {
		return post(u, "/api/relaciones/" + relacionId + "/cargos", """
				{"monto":%s,"descripcion":"cargo de prueba"}""".formatted(monto));
	}

	protected ResultActions pago(Usr u, Long relacionId, String monto) throws Exception {
		return post(u, "/api/relaciones/" + relacionId + "/pagos", """
				{"monto":%s,"medioPago":"TRANSFERENCIA"}""".formatted(monto));
	}

	protected BigDecimal saldo(Usr u, Long relacionId) throws Exception {
		return decimal(get(u, "/api/relaciones/" + relacionId).andExpect(status().isOk()), "$.saldo");
	}

	/** Los push salen en otro hilo: espera (hasta 5 s) a que se cumpla una condición. */
	protected static void esperarHasta(Callable<Boolean> condicion) throws Exception {
		long limite = System.currentTimeMillis() + 5000;
		while (!condicion.call()) {
			if (System.currentTimeMillis() > limite) {
				throw new AssertionError("La condición no se cumplió a tiempo");
			}
			Thread.sleep(50);
		}
	}

	protected static Long idDe(ResultActions resultado) throws Exception {
		return ((Number) JsonPath.read(cuerpo(resultado), "$.id")).longValue();
	}

	protected static BigDecimal decimal(ResultActions resultado, String path) throws Exception {
		return new BigDecimal(JsonPath.read(cuerpo(resultado), path).toString());
	}

	protected static <T> T leer(ResultActions resultado, String path) throws Exception {
		return JsonPath.read(cuerpo(resultado), path);
	}

	protected static String cuerpo(ResultActions resultado) throws Exception {
		return resultado.andReturn().getResponse().getContentAsString();
	}
}
