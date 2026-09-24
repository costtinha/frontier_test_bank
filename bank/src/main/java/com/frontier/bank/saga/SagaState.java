package com.frontier.bank.saga;

/**
 * Estados possíveis de uma saga.
 * <ul>
 *   <li>{@link #RUNNING}: executando passos (ou aguardando nova tentativa);</li>
 *   <li>{@link #COMPLETED}: todos os passos concluídos — é o único estado em que
 *       a operação de negócio é considerada efetivada;</li>
 *   <li>{@link #COMPENSATING}: um passo falhou e os anteriores estão sendo desfeitos;</li>
 *   <li>{@link #COMPENSATED}: desfeita com sucesso — o desfecho de negócio é rejeição,
 *       sem efeito residual;</li>
 *   <li>{@link #FAILED}: <b>exige intervenção humana</b> — não foi possível concluir
 *       nem compensar (pode haver efeito residual).</li>
 * </ul>
 */
public enum SagaState {

	RUNNING,
	COMPLETED,
	COMPENSATING,
	COMPENSATED,
	FAILED;

	public boolean isTerminal() {
		return this != RUNNING && this != COMPENSATING;
	}

}
