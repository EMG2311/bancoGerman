package com.bancogerman.demo.movimiento;

public enum TipoMovimiento {
	/** Baja el saldo. */
	PAGO,
	/** Sube el saldo. Solo lo crea el acreedor. */
	CARGO
}
