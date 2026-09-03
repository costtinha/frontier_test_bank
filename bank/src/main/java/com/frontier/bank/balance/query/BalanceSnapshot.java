package com.frontier.bank.balance.query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Projeção de leitura do saldo (read model). Nunca expõe a entidade de
 * escrita e é hidratada diretamente do banco pelo lado de query.
 */
public record BalanceSnapshot(
		UUID id,
		UUID userId,
		BigDecimal amount,
		Instant createdAt,
		Instant updatedAt) {
}
