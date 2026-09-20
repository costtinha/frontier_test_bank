package com.frontier.bank.transfer.event;

import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.frontier.bank.common.event.EventPayload;

/**
 * Fato: transferência rejeitada por regra de negócio (ex.: saldo insuficiente).
 * <p>
 * É publicado mesmo sem movimentação de dinheiro — nenhum estado é alterado,
 * mas o fato interessa a antifraude, analytics e ao próprio cliente. Erros de
 * sistema (banco fora, usuário inexistente) não geram este evento: eles revertem
 * a transação.
 */
public record TransferFailed(
		UUID transferId,
		UUID sourceUserId,
		UUID targetUserId,
		@JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount,
		String reason) implements EventPayload {

	public static final String TYPE = "bank.transfer.TransferFailed";
	public static final String AGGREGATE = "Transfer";

	@Override
	public UUID aggregateId() {
		return transferId;
	}

	@Override
	public String eventType() {
		return TYPE;
	}

	@Override
	public String aggregateType() {
		return AGGREGATE;
	}

}
