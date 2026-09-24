package com.frontier.bank.transfer.command;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Passo 3 (final) da saga: efetiva a transferência como operação de negócio —
 * persiste o registro e publica {@link com.frontier.bank.transfer.event.TransferCompleted}.
 * <p>
 * Ser um passo (e não um gancho de "conclusão") é intencional: assim a conclusão
 * tem efeito durável, entra no dedupe da saga e não existe estado COMPLETED sem
 * registro correspondente.
 */
public record RegisterCompletedTransferCommand(
		UUID transferId,
		UUID sourceUserId,
		UUID targetUserId,
		BigDecimal amount,
		String idempotencyKey) {
}
