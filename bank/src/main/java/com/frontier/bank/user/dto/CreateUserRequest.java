package com.frontier.bank.user.dto;

import com.frontier.bank.user.UserRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload de criação de usuário.
 *
 * @param role opcional; quando ausente assume {@link UserRole#USER}
 */
public record CreateUserRequest(
		@NotBlank(message = "nome é obrigatório")
		@Size(max = 150, message = "nome deve ter no máximo 150 caracteres")
		String name,

		@NotBlank(message = "email é obrigatório")
		@Email(message = "email inválido")
		@Size(max = 255, message = "email deve ter no máximo 255 caracteres")
		String email,

		@NotBlank(message = "cpf é obrigatório")
		@Pattern(regexp = "\\d{11}", message = "cpf deve conter exatamente 11 dígitos")
		String cpf,

		@NotBlank(message = "senha é obrigatória")
		@Size(min = 8, max = 255, message = "senha deve ter entre 8 e 255 caracteres")
		String password,

		UserRole role) {
}
