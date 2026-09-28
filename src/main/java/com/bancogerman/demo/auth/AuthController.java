package com.bancogerman.demo.auth;

import com.bancogerman.demo.auth.dto.LoginRequest;
import com.bancogerman.demo.auth.dto.RegistroRequest;
import com.bancogerman.demo.auth.dto.TokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/registro")
	@ResponseStatus(HttpStatus.CREATED)
	public TokenResponse registrar(@Valid @RequestBody RegistroRequest request) {
		return authService.registrar(request);
	}

	@PostMapping("/login")
	public TokenResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}
}
