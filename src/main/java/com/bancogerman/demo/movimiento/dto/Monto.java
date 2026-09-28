package com.bancogerman.demo.movimiento.dto;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

/** Monto de dinero válido: obligatorio, positivo, hasta 2 decimales y entra en DECIMAL(15,2). */
@NotNull
@DecimalMin(value = "0.01", message = "debe ser mayor a 0")
@Digits(integer = 13, fraction = 2, message = "máximo 13 enteros y 2 decimales")
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface Monto {

	String message() default "monto inválido";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
