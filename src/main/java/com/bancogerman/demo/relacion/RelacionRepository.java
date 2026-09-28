package com.bancogerman.demo.relacion;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface RelacionRepository extends JpaRepository<Relacion, Long> {

	/** SELECT ... FOR UPDATE: serializa todas las operaciones que tocan el saldo de una relación. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select r from Relacion r where r.id = :id")
	Optional<Relacion> findByIdParaModificar(Long id);

	@Query("""
			select r from Relacion r join fetch r.acreedor join fetch r.deudor
			where r.acreedor.id = :usuarioId and r.estado in :estados
			order by r.creadoEn desc""")
	List<Relacion> findComoAcreedor(Long usuarioId, Collection<EstadoRelacion> estados);

	@Query("""
			select r from Relacion r join fetch r.acreedor join fetch r.deudor
			where r.deudor.id = :usuarioId and r.estado in :estados
			order by r.creadoEn desc""")
	List<Relacion> findComoDeudor(Long usuarioId, Collection<EstadoRelacion> estados);

	boolean existsByAcreedorIdAndDeudorIdAndEstadoIn(Long acreedorId, Long deudorId,
			Collection<EstadoRelacion> estados);
}
