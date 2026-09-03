package com.frontier.bank.user.query;

import java.time.Instant;
import java.util.UUID;

import com.frontier.bank.user.UserRole;

/**
 * Projeção de leitura completa do usuário (detalhe). Nunca expõe a senha.
 */
public record UserDetails(
		UUID id,
		String name,
		String email,
		String cpf,
		UserRole role,
		boolean active,
		Instant createdAt,
		Instant updatedAt) {
}
