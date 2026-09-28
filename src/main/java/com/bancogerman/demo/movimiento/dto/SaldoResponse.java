package com.bancogerman.demo.movimiento.dto;

import java.math.BigDecimal;

/**
 * @param consistente true si el saldo cacheado coincide con el reconstruido desde el ledger
 */
public record SaldoResponse(BigDecimal saldo, BigDecimal saldoSegunLedger, BigDecimal pendienteConfirmacion,
		BigDecimal disponibleParaPagar, boolean consistente) {
}
