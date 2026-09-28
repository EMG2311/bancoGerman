package com.bancogerman.demo.notificacion.push;

public enum ResultadoPush {
	ENVIADO,
	/** El proveedor dice que el token ya no existe (app desinstalada, etc.): hay que borrar el dispositivo. */
	TOKEN_INVALIDO,
	ERROR
}
