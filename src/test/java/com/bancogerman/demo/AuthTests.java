package com.bancogerman.demo;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

class AuthTests extends ApiTestBase {

	@Test
	void registroYLoginDevuelvenToken() throws Exception {
		Usr ana = registrar("Ana");

		mvc.perform(MockMvcRequestBuilders.post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"secreto123"}""".formatted(ana.email().toUpperCase())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isString())
				.andExpect(jsonPath("$.usuario.id").value(ana.id()));

		get(ana, "/api/usuarios/me").andExpect(status().isOk()).andExpect(jsonPath("$.email").value(ana.email()));
	}

	@Test
	void emailDuplicadoDa409() throws Exception {
		Usr ana = registrar("Ana");
		mvc.perform(MockMvcRequestBuilders.post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"nombre":"Otra","email":"%s","password":"secreto123"}""".formatted(ana.email().toUpperCase())))
				.andExpect(status().isConflict());
	}

	@Test
	void passwordIncorrectaDa401() throws Exception {
		Usr ana = registrar("Ana");
		mvc.perform(MockMvcRequestBuilders.post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"incorrecta"}""".formatted(ana.email())))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void sinTokenDa401() throws Exception {
		mvc.perform(MockMvcRequestBuilders.get("/api/relaciones")).andExpect(status().isUnauthorized());
	}

	@Test
	void registroInvalidoDa400ConErroresPorCampo() throws Exception {
		mvc.perform(MockMvcRequestBuilders.post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"nombre":"","email":"no-es-email","password":"corta"}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores.email").exists())
				.andExpect(jsonPath("$.errores.password").exists());
	}
}
