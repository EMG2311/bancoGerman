package com.bancogerman.demo.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
		@NotBlank @Size(max = 100) String nombre,
		@NotBlank @Email @Size(max = 255) String email,
		@Size(max = 30) String telefono,
		// BCrypt solo usa los primeros 72 bytes
		@NotBlank @Size(min = 8, max = 72) String password) {
}
