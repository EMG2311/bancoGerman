package com.bancogerman.demo.auth.dto;

import java.time.Instant;

import com.bancogerman.demo.usuario.dto.UsuarioResponse;

public record TokenResponse(String token, Instant expiraEn, UsuarioResponse usuario) {
}
