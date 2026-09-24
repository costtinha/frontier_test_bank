package com.frontier.bank.transfer.command;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Comando de desfecho negativo da saga: publica {@link com.frontier.bank.transfer.event.TransferFailed}
 * depois que os efeitos foram desfeitos (ou quando não havia efeito nenhum).
 */
public record FailTransferCommand(
		UUID transferId,
		UUID sourceUserId,
		UUID targetUserId,
		BigDecimal amount,
		String reason) {
}
