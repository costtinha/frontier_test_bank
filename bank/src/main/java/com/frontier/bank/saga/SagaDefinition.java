package com.frontier.bank.saga;

import java.util.List;
import java.util.function.BiFunction;

/**
 * Definição de uma saga: seus passos, na ordem de execução, e a ação de desfecho
 * negativo.
 * <p>
 * O passo final costuma ser o que efetiva a operação de negócio (persistir o
 * registro e publicar o evento de conclusão), para que "concluir" não seja um
 * estado sem efeito durável.
 *
 * @param <C> tipo do contexto persistido da saga
 */
public interface SagaDefinition<C> {

	String sagaType();

	Class<C> contextType();

	List<SagaStep<C>> steps();

	/**
	 * Comando executado quando a saga não conclui (após a compensação) — publica o
	 * evento de rejeição. Recebe o contexto e o motivo da falha.
	 * {@code null} quando não há nada a notificar.
	 */
	default BiFunction<C, String, Object> failureCommand() {
		return null;
	}

}
