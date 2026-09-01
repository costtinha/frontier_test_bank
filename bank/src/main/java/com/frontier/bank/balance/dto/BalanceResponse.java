package com.frontier.bank.balance.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Representação pública do saldo do cliente.
 */
public record BalanceResponse(
		UUID id,
		UUID userId,
		BigDecimal amount,
		Instant createdAt,
		Instant updatedAt) {
}
