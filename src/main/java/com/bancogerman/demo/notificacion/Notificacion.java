package com.bancogerman.demo.notificacion;

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

@Entity
@Table(name = "notificacion")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notificacion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id")
	private Usuario usuario;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private TipoNotificacion tipo;

	/** JSON serializado. */
	@Column(nullable = false, columnDefinition = "json")
	private String payload;

	@Column(nullable = false)
	private Instant fecha;

	@Column(nullable = false)
	private boolean leida;

	public Notificacion(Usuario usuario, TipoNotificacion tipo, String payload) {
		this.usuario = usuario;
		this.tipo = tipo;
		this.payload = payload;
		this.fecha = Instant.now();
	}

	public void marcarLeida() {
		this.leida = true;
	}
}
