package com.frontier.bank.transfer.command;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Comando: transferir dinheiro entre dois clientes.
 *
 * @param idempotencyKey chave enviada pelo cliente ({@code Idempotency-Key}) que
 *                       torna o retry seguro
 */
public record TransferMoneyCommand(
		UUID sourceUserId,
		UUID targetUserId,
		BigDecimal amount,
		String idempotencyKey) {
}
