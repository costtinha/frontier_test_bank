package com.frontier.bank.transfer.command;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Passo 2 da saga de transferência: creditar a conta de destino.
 * <p>
 * Não tem compensação própria: se este passo falhar, o efeito a desfazer é o
 * débito do passo anterior.
 */
public record CreditTransferTargetCommand(
		UUID transferId,
		UUID targetUserId,
		BigDecimal amount) {
}
