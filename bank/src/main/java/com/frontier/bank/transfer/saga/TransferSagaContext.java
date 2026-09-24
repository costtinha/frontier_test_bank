package com.frontier.bank.transfer.saga;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Contexto durável da saga de transferência: tudo que qualquer passo precisa
 * para executar (ou compensar) a operação.
 *
 * @param transferId     identidade da operação — é o {@code transactionId} das
 *                       entradas do razão e o id do registro em {@code transfers}
 * @param sourceUserId   cliente de origem
 * @param targetUserId   cliente de destino
 * @param amount         valor transferido
 * @param idempotencyKey chave enviada pelo cliente
 */
public record TransferSagaContext(
		UUID transferId,
		UUID sourceUserId,
		UUID targetUserId,
		BigDecimal amount,
		String idempotencyKey) {

	public boolean matches(UUID sourceUserId, UUID targetUserId, BigDecimal amount) {
		return this.sourceUserId.equals(sourceUserId)
				&& this.targetUserId.equals(targetUserId)
				&& this.amount.compareTo(amount) == 0;
	}

}
