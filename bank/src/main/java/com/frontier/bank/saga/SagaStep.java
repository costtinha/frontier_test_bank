package com.frontier.bank.saga;

import java.util.function.Function;

/**
 * Passo de uma saga: o comando enviado ao participante e, opcionalmente, o
 * comando de compensação que desfaz o efeito do passo.
 *
 * @param name         nome único do passo dentro da saga (chave de idempotência)
 * @param command      constrói o comando do passo a partir do contexto da saga
 * @param compensation constrói o comando de compensação ({@code null} = nada a desfazer)
 * @param <C>          tipo do contexto da saga
 */
public record SagaStep<C>(
		String name,
		Function<C, Object> command,
		Function<C, Object> compensation) {

	public boolean hasCompensation() {
		return compensation != null;
	}

	/** Nome sintético do passo de compensação (guardado na mesma tabela de dedupe). */
	public String compensationName() {
		return name + ":compensation";
	}

}
