package com.bancogerman.demo.relacion;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import com.bancogerman.demo.common.ApiException;
import com.bancogerman.demo.common.Dinero;
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

/**
 * Vínculo dirigido acreedor → deudor. Si dos personas se deben mutuamente son dos relaciones.
 * <p>
 * {@code saldo} es un caché del ledger (Σ cargos confirmados − Σ pagos confirmados) y solo se
 * modifica a través de {@link #aplicarCargo} / {@link #aplicarPago}, siempre con la fila bloqueada.
 */
@Entity
@Table(name = "relacion")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Relacion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "acreedor_id")
	private Usuario acreedor;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "deudor_id")
	private Usuario deudor;

	@Column(nullable = false, precision = 15, scale = 2)
	private BigDecimal saldo;

	@Column(name = "requiere_confirmacion", nullable = false)
	private boolean requiereConfirmacion;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EstadoRelacion estado;

	@Column(name = "creado_en", nullable = false)
	private Instant creadoEn;

	@Column(name = "respondida_en")
	private Instant respondidaEn;

	@Column(name = "eliminada_en")
	private Instant eliminadaEn;

	public Relacion(Usuario acreedor, Usuario deudor, boolean requiereConfirmacion) {
		this.acreedor = acreedor;
		this.deudor = deudor;
		this.requiereConfirmacion = requiereConfirmacion;
		this.saldo = Dinero.normalizar(BigDecimal.ZERO);
		this.estado = EstadoRelacion.INVITADA;
		this.creadoEn = Instant.now();
	}

	public Optional<Rol> rolDe(Long usuarioId) {
		if (acreedor.getId().equals(usuarioId)) {
			return Optional.of(Rol.ACREEDOR);
		}
		if (deudor.getId().equals(usuarioId)) {
			return Optional.of(Rol.DEUDOR);
		}
		return Optional.empty();
	}

	public Usuario usuarioCon(Rol rol) {
		return rol == Rol.ACREEDOR ? acreedor : deudor;
	}

	public void exigirActiva() {
		switch (estado) {
			case ACTIVA -> {
			}
			case INVITADA -> throw ApiException.conflicto("La relación todavía no fue aceptada por el deudor");
			default -> throw ApiException.conflicto("La relación está " + estado.name().toLowerCase());
		}
	}

	public void aceptar() {
		exigirInvitada();
		estado = EstadoRelacion.ACTIVA;
		respondidaEn = Instant.now();
	}

	public void rechazar() {
		exigirInvitada();
		estado = EstadoRelacion.RECHAZADA;
		respondidaEn = Instant.now();
	}

	public void eliminar() {
		if (!estado.esVigente()) {
			throw ApiException.conflicto("La relación ya está " + estado.name().toLowerCase());
		}
		estado = EstadoRelacion.ELIMINADA;
		eliminadaEn = Instant.now();
	}

	public void cambiarRequiereConfirmacion(boolean requiere) {
		if (!estado.esVigente()) {
			throw ApiException.conflicto("La relación está " + estado.name().toLowerCase());
		}
		this.requiereConfirmacion = requiere;
	}

	public void aplicarCargo(BigDecimal monto) {
		saldo = saldo.add(monto);
	}

	/** Piso en 0: el deudor nunca queda a favor. */
	public void aplicarPago(BigDecimal monto) {
		if (monto.compareTo(saldo) > 0) {
			throw ApiException.reglaDeNegocio("El pago de " + monto + " supera la deuda actual de " + saldo);
		}
		saldo = saldo.subtract(monto);
	}

	private void exigirInvitada() {
		if (estado != EstadoRelacion.INVITADA) {
			throw ApiException.conflicto("La invitación ya fue respondida");
		}
	}
}
