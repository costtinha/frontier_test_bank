package com.frontier.bank.user.command;

import com.frontier.bank.user.UserRole;

/**
 * Comando: criar usuário. Dados já validados na camada HTTP.
 */
public record CreateUserCommand(
		String name,
		String email,
		String cpf,
		String password,
		UserRole role) {
}
