package com.bancogerman.demo.relacion;

import com.bancogerman.demo.common.ApiException;
import com.bancogerman.demo.usuario.Usuario;

/**
 * Resultado de pasar por {@link RelacionGuard}: la relación y el lado del usuario logueado.
 */
public record RelacionAcceso(Relacion relacion, Rol rol) {

	/** 403 si el usuario es parte de la relación pero no del lado que la acción exige. */
	public RelacionAcceso exigir(Rol requerido, String accion) {
		if (rol != requerido) {
			throw ApiException.prohibido("Solo el " + requerido.name().toLowerCase() + " puede " + accion);
		}
		return this;
	}

	public Usuario yo() {
		return relacion.usuarioCon(rol);
	}

	public Usuario contraparte() {
		return relacion.usuarioCon(rol == Rol.ACREEDOR ? Rol.DEUDOR : Rol.ACREEDOR);
	}
}
