package com.frontier.bank.transfer.command;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Compensação do passo de débito: devolve o valor à conta de origem.
 * <p>
 * Nome é {@code Refund} (estorno) e não "desfazer": em banco, desfazer dinheiro
 * é sempre um lançamento novo no razão — nunca apagar o anterior.
 */
public record RefundTransferDebitCommand(
		UUID transferId,
		UUID sourceUserId,
		BigDecimal amount) {
}
