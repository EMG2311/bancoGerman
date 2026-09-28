package com.bancogerman.demo.notificacion;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

	Page<Notificacion> findByUsuarioIdOrderByFechaDescIdDesc(Long usuarioId, Pageable pageable);

	Page<Notificacion> findByUsuarioIdAndLeidaFalseOrderByFechaDescIdDesc(Long usuarioId, Pageable pageable);

	Optional<Notificacion> findByIdAndUsuarioId(Long id, Long usuarioId);

	long countByUsuarioIdAndLeidaFalse(Long usuarioId);

	@Modifying
	@Query("update Notificacion n set n.leida = true where n.usuario.id = :usuarioId and n.leida = false")
	int marcarTodasLeidas(Long usuarioId);
}
