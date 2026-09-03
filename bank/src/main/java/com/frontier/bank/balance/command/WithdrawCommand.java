package com.frontier.bank.balance.command;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Comando: sacar dinheiro da conta do cliente.
 */
public record WithdrawCommand(
		UUID userId,
		BigDecimal amount) {
}
