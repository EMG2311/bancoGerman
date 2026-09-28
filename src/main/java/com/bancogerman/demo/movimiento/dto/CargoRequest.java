package com.bancogerman.demo.movimiento.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Size;

public record CargoRequest(@Monto BigDecimal monto, @Size(max = 255) String descripcion) {
}
