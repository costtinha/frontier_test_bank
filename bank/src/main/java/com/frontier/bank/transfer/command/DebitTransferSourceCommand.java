package com.frontier.bank.transfer.command;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Passo 1 da saga de transferência: debitar a conta de origem.
 * <p>
 * É o único passo com compensação (estorno), porque é o único cujo efeito
 * precisa ser desfeito se o crédito não puder ser feito.
 */
public record DebitTransferSourceCommand(
		UUID transferId,
		UUID sourceUserId,
		BigDecimal amount) {
}
