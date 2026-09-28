package com.bancogerman.demo.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

public final class SecurityUtils {

	private SecurityUtils() {
	}

	/** Id del usuario logueado (el "sub" del JWT). */
	public static Long usuarioActualId() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
			throw ApiException.noAutorizado("No autenticado");
		}
		return Long.valueOf(jwt.getSubject());
	}
}
