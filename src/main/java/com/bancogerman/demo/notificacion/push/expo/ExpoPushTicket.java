package com.bancogerman.demo.notificacion.push.expo;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Ticket de un push aceptado por Expo, pendiente de revisar su recibo. */
@Entity
@Table(name = "expo_push_ticket")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpoPushTicket {

	@Id
	@Column(name = "ticket_id", length = 64)
	private String ticketId;

	@Column(name = "push_token", nullable = false, length = 512)
	private String pushToken;

	@Column(name = "creado_en", nullable = false)
	private Instant creadoEn;

	public ExpoPushTicket(String ticketId, String pushToken) {
		this.ticketId = ticketId;
		this.pushToken = pushToken;
		this.creadoEn = Instant.now();
	}
}
