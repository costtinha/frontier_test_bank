package com.frontier.bank.user.command;

import java.util.Locale;
import java.util.UUID;

import com.frontier.bank.common.error.DuplicateFieldException;
import com.frontier.bank.user.UserRepository;

/**
 * Regras de unicidade (email/CPF) compartilhadas pelos handlers de escrita.
 * Uso exclusivo do pacote de comandos.
 */
final class UserUniquenessGuard {

	private final UserRepository repository;

	UserUniquenessGuard(UserRepository repository) {
		this.repository = repository;
	}

	/**
	 * @param currentId id do usuário sendo editado ({@code null} na criação)
	 * @throws DuplicateFieldException se outro usuário já usa o email
	 */
	void ensureEmailAvailable(String email, UUID currentId) {
		String normalized = normalizeEmail(email);
		repository.findByEmail(normalized)
				.filter(existing -> currentId == null || !existing.getId().equals(currentId))
				.ifPresent(existing -> {
					throw new DuplicateFieldException("usuário", "email", normalized);
				});
	}

	void ensureCpfAvailable(String cpf, UUID currentId) {
		repository.findByCpf(cpf)
				.filter(existing -> currentId == null || !existing.getId().equals(currentId))
				.ifPresent(existing -> {
					throw new DuplicateFieldException("usuário", "cpf", cpf);
				});
	}

	String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

}
