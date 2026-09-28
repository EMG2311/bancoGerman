package com.bancogerman.demo.relacion.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** requiereConfirmacion es opcional (default false). */
public record InvitarRequest(@NotBlank @Email String emailDeudor, Boolean requiereConfirmacion) {
}
