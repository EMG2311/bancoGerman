package com.bancogerman.demo.relacion.dto;

import jakarta.validation.constraints.NotNull;

public record CambiarConfirmacionRequest(@NotNull Boolean requiereConfirmacion) {
}
