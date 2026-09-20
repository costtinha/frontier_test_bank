package com.frontier.bank.balance.event;

import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.frontier.bank.common.event.EventPayload;

/**
 * Fato: dinheiro debitado da conta do cliente.
 * <p>
 * Valores monetários são serializados como <b>string</b> no JSON ({@code "50.00"}).
 *
 * @param transactionId    identidade da operação financeira (será persistida no ledger na Fase 3)
 * @param balanceId        conta debitada
 * @param userId           cliente
 * @param amount           valor debitado
 * @param resultingBalance saldo após o débito
 */
public record MoneyWithdrawn(
		UUID transactionId,
		UUID balanceId,
		UUID userId,
		@JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount,
		@JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal resultingBalance) implements EventPayload {

	public static final String TYPE = "bank.balance.MoneyWithdrawn";
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
