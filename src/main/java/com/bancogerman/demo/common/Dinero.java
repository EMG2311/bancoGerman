package com.bancogerman.demo.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Dinero {

	private Dinero() {
	}

	/** Lleva el monto a 2 decimales. La validación de entrada ya garantiza que no hay que redondear. */
	public static BigDecimal normalizar(BigDecimal monto) {
		return monto.setScale(2, RoundingMode.UNNECESSARY);
	}
}
