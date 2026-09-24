package com.frontier.bank.saga;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.frontier.bank.common.command.CommandBus;

/**
 * Executa <b>um</b> passo da saga em transação própria.
 * <p>
 * O comando e o registro de dedupe commitam juntos: se o passo executar e a
 * transação cair antes do commit, nada acontece (o passo será reexecutado); se
 * commitar, o passo não executa de novo. É isso que torna os passos idempotentes
 * sob retry, concorrência entre requisição e sweeper e reinício no meio do avanço.
 * <p>
 * Como cada passo tem sua própria transação, os efeitos de passos anteriores
 * <b>permanecem</b> quando um passo seguinte falha — exatamente o cenário que a
 * compensação existe para resolver.
 */
@Component
public class SagaStepExecutor {

	private final SagaStepExecutionRepository executionRepository;
	private final CommandBus commandBus;

	public SagaStepExecutor(SagaStepExecutionRepository executionRepository, CommandBus commandBus) {
		this.executionRepository = executionRepository;
		this.commandBus = commandBus;
	}

	/**
	 * @return {@code true} se o passo foi executado agora; {@code false} se já
	 *         havia sido executado antes (dedupe)
	 */
	@Transactional
	public boolean execute(UUID sagaId, String stepName, Object command) {
		if (executionRepository.existsBySagaIdAndStepName(sagaId, stepName)) {
			return false;
		}
		Object ignored = commandBus.dispatch(command);
		executionRepository.save(SagaStepExecution.of(sagaId, stepName));
		return true;
	}

}
