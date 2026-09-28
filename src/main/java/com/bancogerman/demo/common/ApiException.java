package com.bancogerman.demo.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Error de negocio o de permisos que se traduce directo a una respuesta HTTP.
 */
@Getter
public class ApiException extends RuntimeException {

	private final HttpStatus status;

	public ApiException(HttpStatus status, String mensaje) {
		super(mensaje);
		this.status = status;
	}

	public static ApiException noEncontrado(String mensaje) {
		return new ApiException(HttpStatus.NOT_FOUND, mensaje);
	}

	public static ApiException prohibido(String mensaje) {
		return new ApiException(HttpStatus.FORBIDDEN, mensaje);
	}

	public static ApiException conflicto(String mensaje) {
		return new ApiException(HttpStatus.CONFLICT, mensaje);
	}

	/** La operación es válida sintácticamente pero viola una regla de negocio (ej. pagar de más). */
	public static ApiException reglaDeNegocio(String mensaje) {
		return new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, mensaje);
	}

	public static ApiException noAutorizado(String mensaje) {
		return new ApiException(HttpStatus.UNAUTHORIZED, mensaje);
	}
}
