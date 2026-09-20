package com.frontier.bank.transfer.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.frontier.bank.transfer.TransferStatus;
import com.frontier.bank.transfer.command.TransferResult;

/**
 * Resposta de uma transferência, montada a partir do desfecho do comando.
 * <p>
 * Não vem do lado de leitura de propósito: uma transferência rejeitada não é
 * persistida (não há linha para consultar), então o desfecho do comando é a
 * fonte correta para a resposta em ambos os casos.
 */
public record TransferView(
		UUID id,
		UUID sourceUserId,
		UUID targetUserId,
		BigDecimal amount,
		TransferStatus status,
		Instant occurredAt) {

	public static TransferView from(TransferResult result) {
		return new TransferView(result.transferId(), result.sourceUserId(), result.targetUserId(),
				result.amount(), result.status(), result.occurredAt());
	}

}
