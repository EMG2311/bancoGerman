package com.bancogerman.demo.cobro;

import java.math.BigDecimal;
import java.time.Instant;

import com.bancogerman.demo.common.ApiException;
import com.bancogerman.demo.common.Dinero;
import com.bancogerman.demo.relacion.Relacion;
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
 * Pedido del acreedor para que el deudor le transfiera. Pasa a PAGADA cuando el pago asociado
 * queda confirmado (en el acto o cuando el acreedor lo confirma).
 */
@Entity
@Table(name = "solicitud_cobro")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SolicitudCobro {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "relacion_id")
	private Relacion relacion;

	@Column(nullable = false, precision = 15, scale = 2)
	private BigDecimal monto;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EstadoSolicitud estado;

	@Column(length = 255)
	private String descripcion;

	@Column(nullable = false)
	private Instant fecha;

	@Column(name = "respondida_en")
	private Instant respondidaEn;

	public SolicitudCobro(Relacion relacion, BigDecimal monto, String descripcion) {
		this.relacion = relacion;
		this.monto = Dinero.normalizar(monto);
		this.descripcion = descripcion;
		this.estado = EstadoSolicitud.PENDIENTE;
		this.fecha = Instant.now();
	}

	public void exigirPendiente() {
		if (estado != EstadoSolicitud.PENDIENTE) {
			throw ApiException.conflicto("La solicitud ya está " + estado.name().toLowerCase());
		}
	}

	public void marcarPagada() {
		exigirPendiente();
		estado = EstadoSolicitud.PAGADA;
		respondidaEn = Instant.now();
	}

	public void rechazar() {
		exigirPendiente();
		estado = EstadoSolicitud.RECHAZADA;
		respondidaEn = Instant.now();
	}
}
