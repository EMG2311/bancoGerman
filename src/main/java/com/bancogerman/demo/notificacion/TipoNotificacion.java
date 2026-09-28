package com.bancogerman.demo.notificacion;

/**
 * Cada tipo trae el texto del push. Los {placeholders} se completan con los datos del evento.
 */
public enum TipoNotificacion {

	INVITACION_RELACION("Nueva invitación", "{actor} te agregó como deudor. Aceptá o rechazá la invitación."),
	RELACION_ACEPTADA("Invitación aceptada", "{actor} aceptó tu invitación."),
	RELACION_RECHAZADA("Invitación rechazada", "{actor} rechazó tu invitación."),
	RELACION_ELIMINADA("Relación eliminada", "{actor} eliminó la relación."),
	CARGO_REGISTRADO("Nuevo cargo", "{actor} registró un cargo de ${monto}."),
	PAGO_REGISTRADO("Pago registrado", "{actor} registró un pago de ${monto} ({medioPago})."),
	PAGO_PENDIENTE("Pago para confirmar", "{actor} registró un pago de ${monto} ({medioPago}). Confirmalo o rechazalo."),
	PAGO_CONFIRMADO("Pago confirmado", "{actor} confirmó tu pago de ${monto}."),
	PAGO_RECHAZADO("Pago rechazado", "{actor} rechazó tu pago de ${monto}."),
	SOLICITUD_COBRO("Solicitud de cobro", "{actor} te pide que le transfieras ${monto}."),
	SOLICITUD_RECHAZADA("Solicitud rechazada", "{actor} rechazó tu solicitud de cobro de ${monto}.");

	private final String titulo;
	private final String plantillaCuerpo;

	TipoNotificacion(String titulo, String plantillaCuerpo) {
		this.titulo = titulo;
		this.plantillaCuerpo = plantillaCuerpo;
	}

	public String titulo() {
		return titulo;
	}

	public String cuerpo(java.util.Map<String, Object> datos) {
		String cuerpo = plantillaCuerpo;
		for (var dato : datos.entrySet()) {
			cuerpo = cuerpo.replace("{" + dato.getKey() + "}", String.valueOf(dato.getValue()));
		}
		return cuerpo;
	}
}
