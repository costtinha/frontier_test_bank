package com.frontier.bank.transfer.event;

import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.frontier.bank.common.event.EventPayload;

/**
 * Fato: transferência concluída — débito e crédito efetivados.
 */
public record TransferCompleted(
		UUID transferId,
		UUID sourceUserId,
		UUID targetUserId,
		@JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount) implements EventPayload {

	public static final String TYPE = "bank.transfer.TransferCompleted";
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
