package com.frontier.bank.saga;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.frontier.bank.common.error.BusinessRejection;
import com.frontier.bank.common.error.ResourceNotFoundException;
import com.frontier.bank.common.observability.BankMetrics;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Orquestrador de sagas: conduz a máquina de estados de cada saga.
 * <p>
 * <b>Avanço</b>: executa os passos em ordem, cada um em sua própria transação
 * (os efeitos de um passo não são revertidos pelo rollback do passo seguinte —
 * por isso existe compensação). <b>Retry</b>: falha transitória agenda nova
 * tentativa com backoff, retomada pelo sweeper. <b>Compensação</b>: rejeição de
 * negócio (ou limite de tentativas) dispara os comandos de compensação em ordem
 * reversa. <b>Intervenção</b>: se nem compensar for possível, a saga termina em
 * {@link SagaState#FAILED} — o único estado que exige ação humana.
 */
@Component
public class SagaOrchestrator {

	private static final Logger log = LoggerFactory.getLogger(SagaOrchestrator.class);

	/** Nome sintético do passo que publica o desfecho negativo (entra no dedupe). */
	private static final String FAILURE_STEP = "onFailed";

	private final SagaInstanceRepository repository;
	private final SagaStepExecutor stepExecutor;
	private final SagaRegistry registry;
	private final ObjectMapper objectMapper;
	private final BankMetrics metrics;
	private final int maxAttempts;
	private final Duration retryBackoff;

	public SagaOrchestrator(SagaInstanceRepository repository, SagaStepExecutor stepExecutor, SagaRegistry registry,
			ObjectMapper objectMapper, BankMetrics metrics,
			@Value("${bank.saga.max-attempts:5}") int maxAttempts,
			@Value("${bank.saga.retry-backoff-seconds:5}") long retryBackoffSeconds) {
		this.repository = repository;
		this.stepExecutor = stepExecutor;
		this.registry = registry;
		this.objectMapper = objectMapper;
		this.metrics = metrics;
		this.maxAttempts = maxAttempts;
		this.retryBackoff = Duration.ofSeconds(retryBackoffSeconds);
	}

	/**
	 * Avança a saga o máximo possível.
	 *
	 * @return o estado da saga após o avanço (terminal ou aguardando nova tentativa)
	 */
	public SagaState advance(UUID sagaId) {
		SagaInstance saga = repository.findById(sagaId)
				.orElseThrow(() -> new ResourceNotFoundException("saga", sagaId));

		if (saga.isTerminal()) {
			return saga.getState();
		}
		return saga.getState() == SagaState.COMPENSATING ? compensate(saga) : run(saga);
	}

	private <C> SagaState run(SagaInstance saga) {
		SagaDefinition<C> definition = definitionOf(saga);
		C context = contextOf(saga, definition);
		List<SagaStep<C>> steps = definition.steps();

		while (saga.getStepIndex() < steps.size()) {
			SagaStep<C> step = steps.get(saga.getStepIndex());
			try {
				stepExecutor.execute(saga.getId(), step.name(), step.command().apply(context));
			} catch (RuntimeException e) {
				if (e instanceof BusinessRejection) {
					metrics.sagaRejected();
					log.info("Saga {} ({}) rejeitada no passo '{}': {}",
							saga.getId(), saga.getSagaType(), step.name(), e.getMessage());
					saga.startCompensation("Passo '%s' rejeitado: %s".formatted(step.name(), e.getMessage()));
					repository.save(saga);
					return compensate(saga);
				}
				return handleTransientFailure(saga, step.name(), e);
			}
			saga.advanceTo(saga.getStepIndex() + 1);
			repository.save(saga);
		}

		saga.complete();
		repository.save(saga);
		metrics.sagaCompleted();
		log.debug("Saga {} ({}) concluída", saga.getId(), saga.getSagaType());
		return saga.getState();
	}

	/** Falha transitória: reagenda ou, esgotadas as tentativas, parte para a compensação. */
	private SagaState handleTransientFailure(SagaInstance saga, String stepName, RuntimeException error) {
		if (saga.getAttempts() + 1 >= maxAttempts) {
			log.error("Saga {} ({}) esgotou {} tentativas no passo '{}': {}",
					saga.getId(), saga.getSagaType(), maxAttempts, stepName, error.getMessage());
			saga.startCompensation("Limite de tentativas excedido no passo '%s': %s"
					.formatted(stepName, error.getMessage()));
			repository.save(saga);
			return compensate(saga);
		}

		Instant nextAttempt = Instant.now().plus(retryBackoff.multipliedBy(saga.getAttempts() + 1L));
		saga.scheduleRetry(nextAttempt, "Passo '%s': %s".formatted(stepName, error.getMessage()));
		repository.save(saga);
		metrics.sagaRetried();
		log.warn("Saga {} reagendada para {} — passo '{}' falhou: {}",
				saga.getId(), nextAttempt, stepName, error.getMessage());
		return saga.getState();
	}

	/** Desfaz, em ordem reversa, os passos já executados e publica o desfecho negativo. */
	private <C> SagaState compensate(SagaInstance saga) {
		SagaDefinition<C> definition = definitionOf(saga);
		C context = contextOf(saga, definition);
		List<SagaStep<C>> steps = definition.steps();

		for (int index = saga.getStepIndex() - 1; index >= 0; index--) {
			SagaStep<C> step = steps.get(index);
			if (!step.hasCompensation()) {
				continue;
			}
			try {
				if (stepExecutor.execute(saga.getId(), step.compensationName(), step.compensation().apply(context))) {
					metrics.sagaCompensationExecuted();
					log.info("Saga {} compensou o passo '{}'", saga.getId(), step.name());
				}
			} catch (RuntimeException e) {
				return handleCompensationFailure(saga, step.name(), e);
			}
		}

		if (definition.failureCommand() != null) {
			try {
				stepExecutor.execute(saga.getId(), FAILURE_STEP,
						definition.failureCommand().apply(context, saga.getLastError()));
			} catch (RuntimeException e) {
				return handleCompensationFailure(saga, FAILURE_STEP, e);
			}
		}

		saga.compensated();
		repository.save(saga);
		metrics.sagaCompensated();
		log.info("Saga {} ({}) compensada sem efeitos residuais: {}",
				saga.getId(), saga.getSagaType(), saga.getLastError());
		return saga.getState();
	}

	private SagaState handleCompensationFailure(SagaInstance saga, String stepName, RuntimeException error) {
		if (error instanceof BusinessRejection || saga.getAttempts() + 1 >= maxAttempts) {
			saga.fail("Falha ao compensar '%s': %s".formatted(stepName, error.getMessage()));
			repository.save(saga);
			metrics.sagaFailed();
			log.error("Saga {} ({}) NÃO conseguiu compensar '{}' — requer intervenção humana: {}",
					saga.getId(), saga.getSagaType(), stepName, error.getMessage());
			return saga.getState();
		}

		Instant nextAttempt = Instant.now().plus(retryBackoff.multipliedBy(saga.getAttempts() + 1L));
		saga.scheduleRetry(nextAttempt, "Compensação de '%s': %s".formatted(stepName, error.getMessage()));
		repository.save(saga);
		metrics.sagaRetried();
		log.warn("Saga {} reagendou a compensação para {}: {}", saga.getId(), nextAttempt, error.getMessage());
		return saga.getState();
	}

	@SuppressWarnings("unchecked")
	private <C> SagaDefinition<C> definitionOf(SagaInstance saga) {
		return (SagaDefinition<C>) registry.definition(saga.getSagaType());
	}

	private <C> C contextOf(SagaInstance saga, SagaDefinition<C> definition) {
		try {
			return objectMapper.readValue(saga.getPayload(), definition.contextType());
		} catch (JacksonException e) {
			throw new IllegalStateException("Contexto inválido da saga " + saga.getId(), e);
		}
	}

}
