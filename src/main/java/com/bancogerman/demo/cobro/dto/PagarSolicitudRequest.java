package com.bancogerman.demo.cobro.dto;

import com.bancogerman.demo.movimiento.MedioPago;
import jakarta.validation.constraints.NotNull;

public record PagarSolicitudRequest(@NotNull MedioPago medioPago) {
}
