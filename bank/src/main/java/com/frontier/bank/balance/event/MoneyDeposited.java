package com.frontier.bank.balance.event;

import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.frontier.bank.common.event.EventPayload;

/**
 * Fato: dinheiro creditado na conta do cliente.
 * <p>
 * Valores monetários são serializados como <b>string</b> no JSON
 * ({@code "100.50"}), evitando perda de precisão em consumidores que usem ponto
 * flutuante. A moeda entra quando o domínio tiver múltiplas moedas (Fase 3, com
 * o ledger).
 *
 * @param transactionId    identidade da operação financeira (será persistida no ledger na Fase 3)
 * @param balanceId        conta creditada
 * @param userId           cliente
 * @param amount           valor creditado
 * @param resultingBalance saldo após o crédito (permite projetar extrato/read model)
 */
public record MoneyDeposited(
		UUID transactionId,
		UUID balanceId,
		UUID userId,
		@JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount,
		@JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal resultingBalance) implements EventPayload {

	public static final String TYPE = "bank.balance.MoneyDeposited";
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
