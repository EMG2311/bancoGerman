package com.bancogerman.demo.notificacion.push.expo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.sun.net.httpserver.HttpServer;

/** Servidor HTTP local que imita la API de push de Expo y guarda lo que recibe. */
class ExpoFalso implements AutoCloseable {

	record Pedido(String ruta, String authorization, String cuerpo) {
	}

	private final HttpServer server;
	final List<Pedido> pedidos = new CopyOnWriteArrayList<>();
	volatile int status = 200;
	volatile String respuesta = "{}";

	ExpoFalso() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", exchange -> {
			pedidos.add(new Pedido(exchange.getRequestURI().getPath(),
					exchange.getRequestHeaders().getFirst("Authorization"),
					new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
			byte[] body = respuesta.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(status, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		server.start();
	}

	String url() {
		return "http://127.0.0.1:" + server.getAddress().getPort();
	}

	@Override
	public void close() {
		server.stop(0);
	}
}
