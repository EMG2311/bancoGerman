package com.bancogerman.demo.usuario.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Campos null = no se tocan. Teléfono vacío = se borra. */
public record ActualizarUsuarioRequest(
		@Size(min = 1, max = 100) @Pattern(regexp = ".*\\S.*", message = "no puede estar vacío") String nombre,
		@Size(max = 30) String telefono) {
}
