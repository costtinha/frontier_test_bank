package com.frontier.bank.transfer.event;

import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.frontier.bank.balance.event.MoneyMovement;
import com.frontier.bank.common.event.EventPayload;

/**
 * Fato: dinheiro debitado da conta de origem de uma transferência.
 * <p>
 * Implementa {@link MoneyMovement}: o read model de saldo trata esta
 * movimentação exatamente como um saque — sempre pelo saldo resultante.
 */
public record TransferDebited(
		UUID transferId,
		UUID balanceId,
		UUID userId,
		@JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount,
		@JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal resultingBalance)
		implements EventPayload, MoneyMovement {

	public static final String TYPE = "bank.transfer.TransferDebited";
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
