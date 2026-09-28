package com.bancogerman.demo.notificacion.push;

/**
 * Abstracción del proveedor de push (FCM, APNs...). Mockeable en tests.
 */
public interface PushSender {

	ResultadoPush enviar(PushMensaje mensaje);
}
