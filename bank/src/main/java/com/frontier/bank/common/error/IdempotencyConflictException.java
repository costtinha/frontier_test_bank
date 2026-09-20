package com.frontier.bank.common.error;

/**
 * Lançada quando uma {@code Idempotency-Key} é reutilizada com parâmetros
 * diferentes da requisição original — HTTP 409.
 */
public class IdempotencyConflictException extends RuntimeException {

	public IdempotencyConflictException(String idempotencyKey) {
		super("Idempotency-Key '%s' já foi utilizada com outra requisição".formatted(idempotencyKey));
	}

}
