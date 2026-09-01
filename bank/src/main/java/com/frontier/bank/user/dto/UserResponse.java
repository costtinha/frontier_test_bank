package com.frontier.bank.user.dto;

import java.time.Instant;
import java.util.UUID;

import com.frontier.bank.user.UserRole;

/**
 * Representação pública do usuário — nunca expõe a senha.
 */
public record UserResponse(
		UUID id,
		String name,
		String email,
		String cpf,
		UserRole role,
		boolean active,
		Instant createdAt,
		Instant updatedAt) {
}
