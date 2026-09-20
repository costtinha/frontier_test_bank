package com.frontier.bank.balance.event;

import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.frontier.bank.common.event.EventPayload;

/**
 * Fato: conta de saldo aberta para o cliente (nasce com R$ 0,00).
 * <p>
 * É o evento que dá origem à linha do read model de saldo — sem ele, um cliente
 * sem movimentação não existiria no lado de leitura.
 *
 * @param balanceId conta aberta
 * @param userId    cliente
 * @param amount    saldo inicial (sempre zero hoje)
 */
public record BalanceOpened(
		UUID balanceId,
		UUID userId,
		@JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount) implements EventPayload {

	public static final String TYPE = "bank.balance.BalanceOpened";
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
