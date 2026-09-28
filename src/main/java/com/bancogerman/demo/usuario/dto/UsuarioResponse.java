package com.bancogerman.demo.usuario.dto;

import com.bancogerman.demo.usuario.Usuario;

public record UsuarioResponse(Long id, String nombre, String email, String telefono) {

	public static UsuarioResponse de(Usuario usuario) {
		return new UsuarioResponse(usuario.getId(), usuario.getNombre(), usuario.getEmail(), usuario.getTelefono());
	}
}
