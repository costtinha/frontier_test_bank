package com.frontier.bank.transfer.command;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.frontier.bank.transfer.Transfer;
import com.frontier.bank.transfer.TransferStatus;

/**
 * Desfecho de uma tentativa de transferência.
 * <p>
 * Rejeição por regra de negócio (saldo insuficiente) <b>não</b> é exceção: é um
 * resultado possível que precisa ser registrado como fato (evento
 * {@code TransferFailed}) sem alterar saldo. Quem traduz isso em HTTP é o controller.
 *
 * @param failureReason motivo legível quando {@code status = FAILED}
 */
public record TransferResult(
		UUID transferId,
		TransferStatus status,
		UUID sourceUserId,
		UUID targetUserId,
		BigDecimal amount,
		Instant occurredAt,
		String failureReason) {

	public static TransferResult completed(UUID transferId, UUID sourceUserId, UUID targetUserId,
			BigDecimal amount, Instant occurredAt) {
		return new TransferResult(transferId, TransferStatus.COMPLETED, sourceUserId, targetUserId, amount,
				occurredAt, null);
	}

	/** Retry idempotente: devolve o desfecho já registrado, sem reexecutar nada. */
	public static TransferResult replayed(Transfer transfer) {
		return new TransferResult(transfer.getId(), transfer.getStatus(), transfer.getSourceUserId(),
				transfer.getTargetUserId(), transfer.getAmount(), transfer.getCreatedAt(), null);
	}

	public static TransferResult failed(UUID transferId, UUID sourceUserId, UUID targetUserId,
			BigDecimal amount, Instant occurredAt, String failureReason) {
		return new TransferResult(transferId, TransferStatus.FAILED, sourceUserId, targetUserId, amount,
				occurredAt, failureReason);
	}

	public boolean isFailed() {
		return status == TransferStatus.FAILED;
	}

}
