package com.frontier.bank.user.query;

import java.time.Instant;
import java.util.UUID;

import com.frontier.bank.user.UserRole;

/**
 * Projeção de leitura para listagens: enxuta, sem dados sensíveis (CPF/senha).
 * Campos escolhidos para a tela de listagem.
 */
public record UserSummary(
		UUID id,
		String name,
		String email,
		UserRole role,
		boolean active,
		Instant createdAt) {
}
