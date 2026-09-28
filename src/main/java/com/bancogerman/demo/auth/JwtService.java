package com.bancogerman.demo.auth;

import java.time.Instant;

import com.bancogerman.demo.auth.dto.TokenResponse;
import com.bancogerman.demo.config.JwtProperties;
import com.bancogerman.demo.usuario.Usuario;
import com.bancogerman.demo.usuario.dto.UsuarioResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtService {

	private final JwtEncoder jwtEncoder;
	private final JwtProperties props;

	public TokenResponse emitir(Usuario usuario) {
		Instant ahora = Instant.now();
		Instant expira = ahora.plus(props.expiracion());
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject(usuario.getId().toString())
				.claim("email", usuario.getEmail())
				.issuedAt(ahora)
				.expiresAt(expira)
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
		return new TokenResponse(token, expira, UsuarioResponse.de(usuario));
	}
}
