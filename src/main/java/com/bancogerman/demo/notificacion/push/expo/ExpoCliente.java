package com.bancogerman.demo.notificacion.push.expo;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** POST JSON contra la API de push de Expo. Cualquier falla se loguea y devuelve vacío. */
@Slf4j
public class ExpoCliente {

	private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
	private final ExpoProperties props;
	private final JsonMapper jsonMapper;

	public ExpoCliente(ExpoProperties props, JsonMapper jsonMapper) {
		this.props = props;
		this.jsonMapper = jsonMapper;
	}

	public Optional<JsonNode> post(String ruta, Object cuerpo) {
		HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(props.url() + ruta))
				.timeout(Duration.ofSeconds(15))
				.header("Accept", "application/json")
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(jsonMapper.writeValueAsString(cuerpo)));
		if (props.accessToken() != null && !props.accessToken().isBlank()) {
			request.header("Authorization", "Bearer " + props.accessToken());
		}
		try {
			HttpResponse<String> respuesta = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
			if (respuesta.statusCode() / 100 != 2) {
				log.warn("Expo respondió {} en {}: {}", respuesta.statusCode(), ruta, respuesta.body());
				return Optional.empty();
			}
			return Optional.of(jsonMapper.readTree(respuesta.body()));
		} catch (IOException | JacksonException e) {
			log.warn("No se pudo hablar con Expo ({})", ruta, e);
			return Optional.empty();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return Optional.empty();
		}
	}
}
