package com.bancogerman.demo.notificacion.push.expo;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface ExpoPushTicketRepository extends JpaRepository<ExpoPushTicket, String> {

	/** Hasta 1000 por vuelta: es el máximo de ids que acepta /push/getReceipts. */
	List<ExpoPushTicket> findTop1000ByCreadoEnBeforeOrderByCreadoEnAsc(Instant limite);

	@Modifying
	@Transactional
	@Query("delete from ExpoPushTicket t where t.creadoEn < :limite")
	int borrarAnterioresA(Instant limite);
}
