package com.bancogerman.demo.dispositivo;

import java.time.Instant;

import com.bancogerman.demo.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Teléfono/navegador del usuario. Está atado al usuario, no a una relación. */
@Entity
@Table(name = "dispositivo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Dispositivo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id")
	private Usuario usuario;

	@Column(name = "push_token", nullable = false, unique = true, length = 512)
	private String pushToken;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Plataforma plataforma;

	@Column(name = "creado_en", nullable = false)
	private Instant creadoEn;

	@Column(name = "actualizado_en", nullable = false)
	private Instant actualizadoEn;

	public Dispositivo(Usuario usuario, String pushToken, Plataforma plataforma) {
		this.usuario = usuario;
		this.pushToken = pushToken;
		this.plataforma = plataforma;
		this.creadoEn = Instant.now();
		this.actualizadoEn = this.creadoEn;
	}

	/** Si otra cuenta inicia sesión en el mismo teléfono, el token pasa a ser de esa cuenta. */
	public void asignarA(Usuario usuario, Plataforma plataforma) {
		this.usuario = usuario;
		this.plataforma = plataforma;
		this.actualizadoEn = Instant.now();
	}
}
