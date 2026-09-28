package com.bancogerman.demo.common;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ApiException.class)
	public ProblemDetail manejarApi(ApiException e) {
		return ProblemDetail.forStatusAndDetail(e.getStatus(), e.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail manejarValidacion(MethodArgumentNotValidException e) {
		Map<String, String> errores = new LinkedHashMap<>();
		for (FieldError error : e.getBindingResult().getFieldErrors()) {
			errores.putIfAbsent(error.getField(), error.getDefaultMessage());
		}
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Datos inválidos");
		problema.setProperty("errores", errores);
		return problema;
	}

	@ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
	public ProblemDetail manejarRequestIlegible(Exception e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request mal formado");
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ProblemDetail manejarIntegridad(DataIntegrityViolationException e) {
		log.warn("Violación de integridad", e);
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "La operación entra en conflicto con datos existentes");
	}
}
