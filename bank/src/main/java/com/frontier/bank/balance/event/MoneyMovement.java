package com.frontier.bank.balance.event;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Contrato comum dos eventos de movimentação de dinheiro
 * ({@link MoneyDeposited}, {@link MoneyWithdrawn}).
 * <p>
 * Projetores de extrato/saldo tratam crédito e débito de forma uniforme: o
 * estado resultante vem sempre de {@code resultingBalance}, nunca de uma soma
 * incremental — isso torna a projeção idempotente e tolerante a reordenação.
 */
public interface MoneyMovement {

	UUID balanceId();

	UUID userId();

	BigDecimal amount();

	BigDecimal resultingBalance();

}
