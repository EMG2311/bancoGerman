package com.bancogerman.demo.movimiento;

import java.math.BigDecimal;
import java.time.Instant;

import com.bancogerman.demo.cobro.SolicitudCobro;
import com.bancogerman.demo.common.ApiException;
import com.bancogerman.demo.common.Dinero;
import com.bancogerman.demo.relacion.Relacion;
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
 * Asiento del libro mayor de una relación. Los CONFIRMADOS son la fuente de verdad del saldo.
 */
@Entity
@Table(name = "movimiento")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Movimiento {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "relacion_id")
	private Relacion relacion;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TipoMovimiento tipo;

	@Column(nullable = false, precision = 15, scale = 2)
	private BigDecimal monto;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EstadoMovimiento estado;

	@Enumerated(EnumType.STRING)
	@Column(name = "medio_pago")
	private MedioPago medioPago;

	/** Si al nacer necesitaba confirmación. Cambiar el flag de la relación después no lo afecta. */
	@Column(name = "requirio_confirmacion", nullable = false)
	private boolean requirioConfirmacion;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "creado_por_id")
	private Usuario creadoPor;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "solicitud_id")
	private SolicitudCobro solicitud;

	@Column(length = 255)
	private String descripcion;

	@Column(nullable = false)
	private Instant fecha;

	/** Cuándo impactó en el saldo o se rechazó. */
	@Column(name = "resuelto_en")
	private Instant resueltoEn;

	private Movimiento(Relacion relacion, TipoMovimiento tipo, BigDecimal monto, MedioPago medioPago,
			boolean requirioConfirmacion, Usuario creadoPor, SolicitudCobro solicitud, String descripcion) {
		this.relacion = relacion;
		this.tipo = tipo;
		this.monto = Dinero.normalizar(monto);
		this.medioPago = medioPago;
		this.requirioConfirmacion = requirioConfirmacion;
		this.creadoPor = creadoPor;
		this.solicitud = solicitud;
		this.descripcion = descripcion;
		this.fecha = Instant.now();
		if (requirioConfirmacion) {
			this.estado = EstadoMovimiento.PENDIENTE;
		} else {
			this.estado = EstadoMovimiento.CONFIRMADO;
			this.resueltoEn = this.fecha;
		}
	}

	public static Movimiento cargo(Relacion relacion, Usuario acreedor, BigDecimal monto, String descripcion) {
		return new Movimiento(relacion, TipoMovimiento.CARGO, monto, null, false, acreedor, null, descripcion);
	}

	public static Movimiento pago(Relacion relacion, Usuario creadoPor, BigDecimal monto, MedioPago medio,
			boolean requiereConfirmacion, SolicitudCobro solicitud, String descripcion) {
		return new Movimiento(relacion, TipoMovimiento.PAGO, monto, medio, requiereConfirmacion, creadoPor,
				solicitud, descripcion);
	}

	public boolean estaConfirmado() {
		return estado == EstadoMovimiento.CONFIRMADO;
	}

	public void confirmar() {
		exigirPendiente();
		estado = EstadoMovimiento.CONFIRMADO;
		resueltoEn = Instant.now();
	}

	public void rechazar() {
		exigirPendiente();
		estado = EstadoMovimiento.RECHAZADO;
		resueltoEn = Instant.now();
	}

	private void exigirPendiente() {
		if (estado != EstadoMovimiento.PENDIENTE) {
			throw ApiException.conflicto("El movimiento ya está " + estado.name().toLowerCase());
		}
	}
}
