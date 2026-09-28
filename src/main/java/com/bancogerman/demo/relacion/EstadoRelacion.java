package com.bancogerman.demo.relacion;

public enum EstadoRelacion {
	/** El acreedor invitó; el deudor todavía no respondió. No admite movimientos. */
	INVITADA,
	ACTIVA,
	/** El deudor rechazó la invitación. Estado final. */
	RECHAZADA,
	/** Alguna de las partes la eliminó (borrado lógico: el historial se conserva). Estado final. */
	ELIMINADA;

	public boolean esVigente() {
		return this == INVITADA || this == ACTIVA;
	}
}
