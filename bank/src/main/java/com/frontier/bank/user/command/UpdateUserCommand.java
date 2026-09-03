package com.frontier.bank.user.command;

import java.util.UUID;

import com.frontier.bank.user.UserRole;

/**
 * Comando: atualizar parcialmente um usuário. Campos {@code null} não alteram.
 */
public record UpdateUserCommand(
		UUID id,
		String name,
		String email,
		String password,
		UserRole role,
		Boolean active) {
}
