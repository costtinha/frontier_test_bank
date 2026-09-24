package com.frontier.bank.transfer.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.frontier.bank.transfer.TransferOutcome;
import com.frontier.bank.transfer.command.TransferResult;

/**
 * Resposta de uma transferência, montada a partir do desfecho da saga.
 * <p>
 * Não vem do lado de leitura de propósito: uma transferência rejeitada ou ainda
 * em processamento não é persistida em {@code transfers} (não há linha para
 * consultar), então o desfecho da saga é a fonte correta em todos os casos.
 *
 * @param id       id da operação (é o id do registro em {@code transfers} quando concluída)
 * @param outcome  desfecho: {@code COMPLETED}, {@code REJECTED}, {@code PENDING} ou {@code FAILED}
 */
public record TransferView(
		UUID id,
		UUID sourceUserId,
		UUID targetUserId,
		BigDecimal amount,
		TransferOutcome outcome,
		Instant occurredAt,
		String failureReason) {

	public static TransferView from(TransferResult result) {
		return new TransferView(result.transferId(), result.sourceUserId(), result.targetUserId(),
				result.amount(), result.outcome(), result.occurredAt(), result.failureReason());
	}

}
