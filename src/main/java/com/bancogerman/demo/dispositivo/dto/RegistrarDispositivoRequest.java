package com.bancogerman.demo.dispositivo.dto;

import com.bancogerman.demo.dispositivo.Plataforma;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegistrarDispositivoRequest(@NotBlank @Size(max = 512) String pushToken,
		@NotNull Plataforma plataforma) {
}
