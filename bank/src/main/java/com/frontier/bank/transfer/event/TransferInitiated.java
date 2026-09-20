package com.frontier.bank.transfer.event;

import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.frontier.bank.common.event.EventPayload;

/**
 * Fato: transferência aceita para processamento.
 * <p>
 * No fluxo atual (atômico, dentro de uma única transação) este evento e os
 * demais são commitados juntos. Quando as contas forem separadas em serviços,
 * ele passa a ser o primeiro passo durável da saga — por isso já existe agora.
 *
 * @param transferId     identificador da operação (agrupa as duas entradas do razão)
 * @param sourceUserId   cliente de origem
 * @param targetUserId   cliente de destino
 * @param amount         valor transferido
 */
public record TransferInitiated(
		UUID transferId,
		UUID sourceUserId,
		UUID targetUserId,
		@JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount) implements EventPayload {

	public static final String TYPE = "bank.transfer.TransferInitiated";
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
