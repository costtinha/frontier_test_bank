package com.frontier.bank.user.dto;

import com.frontier.bank.user.UserRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Payload de atualização parcial (PATCH): campos {@code null} não são alterados.
 */
public record UpdateUserRequest(
		@Size(max = 150, message = "nome deve ter no máximo 150 caracteres")
		String name,

		@Email(message = "email inválido")
		@Size(max = 255, message = "email deve ter no máximo 255 caracteres")
		String email,

		@Size(min = 8, max = 255, message = "senha deve ter entre 8 e 255 caracteres")
		String password,

		UserRole role,

		Boolean active) {
}
