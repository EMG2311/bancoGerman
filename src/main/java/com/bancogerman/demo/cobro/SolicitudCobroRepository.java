package com.bancogerman.demo.cobro;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SolicitudCobroRepository extends JpaRepository<SolicitudCobro, Long> {

	List<SolicitudCobro> findByRelacionIdOrderByFechaDescIdDesc(Long relacionId);

	List<SolicitudCobro> findByRelacionIdAndEstadoOrderByFechaDescIdDesc(Long relacionId, EstadoSolicitud estado);

	List<SolicitudCobro> findByRelacionIdAndEstado(Long relacionId, EstadoSolicitud estado);

	@Query("select s.relacion.id from SolicitudCobro s where s.id = :solicitudId")
	Optional<Long> findRelacionIdDe(Long solicitudId);
}
