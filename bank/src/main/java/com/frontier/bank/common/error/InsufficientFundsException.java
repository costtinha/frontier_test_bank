package com.frontier.bank.common.error;

import java.math.BigDecimal;

/**
 * Lançada quando um saque excede o saldo disponível — HTTP 422.
 */
public class InsufficientFundsException extends RuntimeException {

	public InsufficientFundsException(BigDecimal available, BigDecimal requested) {
		super("Saldo insuficiente: disponível %s, solicitado %s".formatted(available, requested));
	}

}
