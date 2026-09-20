package com.frontier.bank.common.error;

/**
 * Lançada quando uma transferência é rejeitada por regra de negócio
 * (ex.: saldo insuficiente) — HTTP 422.
 */
public class TransferRejectedException extends RuntimeException {

	public TransferRejectedException(String reason) {
		super(reason);
	}

}
