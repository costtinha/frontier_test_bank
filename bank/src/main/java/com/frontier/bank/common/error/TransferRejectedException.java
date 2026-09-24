package com.frontier.bank.common.error;

/**
 * Lançada quando uma transferência é rejeitada por regra de negócio
 * (ex.: saldo insuficiente) — HTTP 422.
 * <p>
 * É uma {@link BusinessRejection}: repetir não muda o resultado, então a saga
 * compensa em vez de tentar de novo.
 */
public class TransferRejectedException extends RuntimeException implements BusinessRejection {

	public TransferRejectedException(String reason) {
		super(reason);
	}

}
