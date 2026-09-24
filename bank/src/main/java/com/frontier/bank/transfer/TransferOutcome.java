package com.frontier.bank.transfer;

/**
 * Desfecho de uma tentativa de transferência do ponto de vista do cliente.
 * <p>
 * Separado de {@link TransferStatus} (que é o estado persistido do registro de
 * transferência concluída) porque a saga tem desfechos que não viram linha em
 * {@code transfers}: {@link #PENDING} (ainda processando) e {@link #FAILED}
 * (não concluiu nem conseguiu compensar — requer intervenção).
 */
public enum TransferOutcome {

	/** Todos os passos concluídos. */
	COMPLETED,

	/** Rejeitada por regra de negócio, com efeitos desfeitos (compensada). */
	REJECTED,

	/** Saga ainda em execução ou aguardando nova tentativa. */
	PENDING,

	/** Não concluiu nem compensou: pode haver efeito residual (intervenção humana). */
	FAILED

}
