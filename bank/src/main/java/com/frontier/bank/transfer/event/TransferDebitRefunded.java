package com.frontier.bank.transfer.event;

import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.frontier.bank.balance.event.MoneyMovement;
import com.frontier.bank.common.event.EventPayload;

/**
 * Fato: o débito de uma transferência foi <b>estornado</b> (compensação da saga
 * quando o crédito não pôde ser feito).
 * <p>
 * Implementa {@link MoneyMovement}, então o read model de saldo trata o estorno
 * como qualquer crédito — sem lógica nova no projector.
 */
public record TransferDebitRefunded(
		UUID transferId,
		UUID balanceId,
		UUID userId,
		@JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount,
		@JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal resultingBalance)
		implements EventPayload, MoneyMovement {

	public static final String TYPE = "bank.transfer.TransferDebitRefunded";
	public static final String AGGREGATE = "Balance";

	@Override
	public UUID aggregateId() {
		return balanceId;
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
