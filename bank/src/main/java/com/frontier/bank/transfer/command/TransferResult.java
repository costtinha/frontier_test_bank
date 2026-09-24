package com.frontier.bank.transfer.command;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.frontier.bank.transfer.TransferOutcome;

/**
 * Desfecho de uma tentativa de transferência conduzida pela saga.
 * <p>
 * Rejeição de negócio <b>não</b> é exceção: é um resultado possível que precisa
 * ser registrado como fato sem alterar saldo. Quem traduz isso em HTTP é o controller.
 *
 * @param failureReason motivo legível quando o desfecho não é {@code COMPLETED}
 */
public record TransferResult(
		UUID transferId,
		TransferOutcome outcome,
		UUID sourceUserId,
		UUID targetUserId,
		BigDecimal amount,
		Instant occurredAt,
		String failureReason) {

	public static TransferResult completed(UUID transferId, UUID sourceUserId, UUID targetUserId,
			BigDecimal amount, Instant occurredAt) {
		return new TransferResult(transferId, TransferOutcome.COMPLETED, sourceUserId, targetUserId, amount,
				occurredAt, null);
	}

	public static TransferResult rejected(UUID transferId, UUID sourceUserId, UUID targetUserId,
			BigDecimal amount, Instant occurredAt, String reason) {
		return new TransferResult(transferId, TransferOutcome.REJECTED, sourceUserId, targetUserId, amount,
				occurredAt, reason);
	}

	public static TransferResult pending(UUID transferId, UUID sourceUserId, UUID targetUserId,
			BigDecimal amount, Instant occurredAt) {
		return new TransferResult(transferId, TransferOutcome.PENDING, sourceUserId, targetUserId, amount,
				occurredAt, "Transferência em processamento pela saga");
	}

	public static TransferResult failed(UUID transferId, UUID sourceUserId, UUID targetUserId,
			BigDecimal amount, Instant occurredAt, String reason) {
		return new TransferResult(transferId, TransferOutcome.FAILED, sourceUserId, targetUserId, amount,
				occurredAt, reason);
	}

	public boolean isPending() {
		return outcome == TransferOutcome.PENDING;
	}

	public boolean isRejected() {
		return outcome == TransferOutcome.REJECTED;
	}

	public boolean requiresIntervention() {
		return outcome == TransferOutcome.FAILED;
	}

}
