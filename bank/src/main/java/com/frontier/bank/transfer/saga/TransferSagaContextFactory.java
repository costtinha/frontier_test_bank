package com.frontier.bank.transfer.saga;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import com.frontier.bank.transfer.command.TransferMoneyCommand;

/**
 * Cria o contexto da saga normalizando o valor (2 casas, HALF_UP) — o mesmo
 * critério usado pelos handlers de dinheiro.
 */
final class TransferSagaContextFactory {

	private TransferSagaContextFactory() {
	}

	static TransferSagaContext of(TransferMoneyCommand command) {
		return new TransferSagaContext(
				UUID.randomUUID(),
				command.sourceUserId(),
				command.targetUserId(),
				command.amount().setScale(2, RoundingMode.HALF_UP),
				command.idempotencyKey());
	}

	static BigDecimal normalize(BigDecimal amount) {
		return amount.setScale(2, RoundingMode.HALF_UP);
	}

}
