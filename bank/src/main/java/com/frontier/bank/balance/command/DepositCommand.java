package com.frontier.bank.balance.command;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Comando: depositar dinheiro na conta do cliente.
 */
public record DepositCommand(
		UUID userId,
		BigDecimal amount) {
}
