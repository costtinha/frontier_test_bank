package com.frontier.bank.transfer;

/**
 * Estado da transferência.
 * <p>
 * Hoje o fluxo é atômico: só transferências concluídas são persistidas
 * ({@code COMPLETED}). {@code FAILED} existe para o desfecho de uma tentativa
 * rejeitada (registrado no evento {@code TransferFailed}) e para o estado que a
 * saga passará a usar quando as contas estiverem em serviços separados.
 */
public enum TransferStatus {

	COMPLETED,
	FAILED

}
