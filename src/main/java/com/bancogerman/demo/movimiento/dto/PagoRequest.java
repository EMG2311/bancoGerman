package com.bancogerman.demo.movimiento.dto;

import java.math.BigDecimal;

import com.bancogerman.demo.movimiento.MedioPago;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PagoRequest(@Monto BigDecimal monto, @NotNull MedioPago medioPago, @Size(max = 255) String descripcion) {
}
