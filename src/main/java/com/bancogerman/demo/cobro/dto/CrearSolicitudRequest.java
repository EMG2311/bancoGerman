package com.bancogerman.demo.cobro.dto;

import java.math.BigDecimal;

import com.bancogerman.demo.movimiento.dto.Monto;
import jakarta.validation.constraints.Size;

public record CrearSolicitudRequest(@Monto BigDecimal monto, @Size(max = 255) String descripcion) {
}
