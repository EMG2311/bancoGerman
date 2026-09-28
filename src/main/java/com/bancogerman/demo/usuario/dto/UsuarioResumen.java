package com.bancogerman.demo.usuario.dto;

import com.bancogerman.demo.usuario.Usuario;

/** Lo que se muestra de la contraparte dentro de una relación. */
public record UsuarioResumen(Long id, String nombre, String email) {

	public static UsuarioResumen de(Usuario usuario) {
		return new UsuarioResumen(usuario.getId(), usuario.getNombre(), usuario.getEmail());
	}
}
