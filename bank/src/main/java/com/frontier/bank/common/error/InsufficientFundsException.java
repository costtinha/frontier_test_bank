package com.frontier.bank.common.error;

import java.math.BigDecimal;

/**
 * Lançada quando um saque excede o saldo disponível — HTTP 422.
 * <p>
 * É uma {@link BusinessRejection}: a saga compensa em vez de repetir.
 */
public class InsufficientFundsException extends RuntimeException implements BusinessRejection {

	public InsufficientFundsException(BigDecimal available, BigDecimal requested) {
		super("Saldo insuficiente: disponível %s, solicitado %s".formatted(available, requested));
	}

}
