package com.bancogerman.demo.usuario;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Usuario sin rol fijo: es acreedor o deudor según la relación que se mire.
 */
@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String nombre;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(length = 30)
	private String telefono;

	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	@Column(name = "creado_en", nullable = false)
	private Instant creadoEn;

	public Usuario(String nombre, String email, String telefono, String passwordHash) {
		this.nombre = nombre;
		this.email = email;
		this.telefono = telefono;
		this.passwordHash = passwordHash;
		this.creadoEn = Instant.now();
	}
}
