package com.bancogerman.demo.dispositivo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface DispositivoRepository extends JpaRepository<Dispositivo, Long> {

	List<Dispositivo> findByUsuarioId(Long usuarioId);

	Optional<Dispositivo> findByPushToken(String pushToken);

	Optional<Dispositivo> findByIdAndUsuarioId(Long id, Long usuarioId);

	@Modifying
	@Transactional
	@Query("delete from Dispositivo d where d.pushToken = :pushToken")
	int borrarPorToken(String pushToken);
}
