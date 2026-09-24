package com.frontier.bank.saga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.frontier.bank.common.error.TransferRejectedException;
import com.frontier.bank.common.observability.BankMetrics;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class SagaOrchestratorTest {

	private static final String SAGA_TYPE = "TestSaga";
	private static final int MAX_ATTEMPTS = 3;
	private static final String STEP_1 = "step1";
	private static final String STEP_2 = "step2";
	private static final String COMPENSATION_1 = "step1:compensation";
	private static final String FAILURE_STEP = "onFailed";

	@Mock
	private SagaInstanceRepository repository;

	@Mock
	private SagaStepExecutor stepExecutor;

	@Mock
	private SagaRegistry registry;

	@Mock
	private BankMetrics metrics;

	private final ObjectMapper objectMapper = new ObjectMapper();

	/** Saga de teste: passo 1 com compensação, passo 2 sem. */
	private final SagaDefinition<TestContext> definition = new SagaDefinition<>() {

		@Override
		public String sagaType() {
			return SAGA_TYPE;
		}

		@Override
		public Class<TestContext> contextType() {
			return TestContext.class;
		}

		@Override
		public List<SagaStep<TestContext>> steps() {
			return List.of(
					new SagaStep<>(STEP_1, context -> new TestCommand(STEP_1),
							context -> new TestCommand(COMPENSATION_1)),
					new SagaStep<>(STEP_2, context -> new TestCommand(STEP_2), null));
		}

		@Override
		public BiFunction<TestContext, String, Object> failureCommand() {
			return (context, reason) -> new TestCommand("fail");
		}
	};

	private SagaOrchestrator orchestrator;

	@BeforeEach
	void setUp() {
		orchestrator = new SagaOrchestrator(repository, stepExecutor, registry, objectMapper, metrics,
				MAX_ATTEMPTS, 5);
	}

	@Test
	void shouldCompleteSagaRunningAllSteps() {
		SagaInstance saga = givenSagaInProgress();
		givenDefinitionRegistered();
		when(stepExecutor.execute(eq(saga.getId()), anyString(), any())).thenReturn(true);

		SagaState state = orchestrator.advance(saga.getId());

		assertThat(state).isEqualTo(SagaState.COMPLETED);
		assertThat(saga.getStepIndex()).isEqualTo(2);
		verify(stepExecutor).execute(saga.getId(), STEP_1, new TestCommand(STEP_1));
		verify(stepExecutor).execute(saga.getId(), STEP_2, new TestCommand(STEP_2));
		verify(stepExecutor, never()).execute(any(), eq(COMPENSATION_1), any());
		verify(metrics).sagaCompleted();
	}

	@Test
	void shouldSkipStepAlreadyExecuted() {
		SagaInstance saga = givenSagaInProgress();
		givenDefinitionRegistered();
		// dedupe: o passo já havia sido executado em tentativa anterior
		when(stepExecutor.execute(eq(saga.getId()), anyString(), any())).thenReturn(false);

		SagaState state = orchestrator.advance(saga.getId());

		assertThat(state).isEqualTo(SagaState.COMPLETED);
		verify(metrics).sagaCompleted();
	}

	@Test
	void shouldCompensateExecutedStepsWhenStepIsRejected() {
		SagaInstance saga = givenSagaInProgress();
		givenDefinitionRegistered();
		when(stepExecutor.execute(eq(saga.getId()), eq(STEP_1), any())).thenReturn(true);
		when(stepExecutor.execute(eq(saga.getId()), eq(STEP_2), any()))
				.thenThrow(new TransferRejectedException("Saldo insuficiente"));

		SagaState state = orchestrator.advance(saga.getId());

		assertThat(state).isEqualTo(SagaState.COMPENSATED);
		// o efeito do passo 1 é desfeito e o desfecho negativo é publicado
		verify(stepExecutor).execute(saga.getId(), COMPENSATION_1, new TestCommand(COMPENSATION_1));
		verify(stepExecutor).execute(saga.getId(), FAILURE_STEP, new TestCommand("fail"));
		verify(metrics).sagaRejected();
		verify(metrics).sagaCompensated();
		verify(metrics, never()).sagaFailed();
	}

	@Test
	void shouldScheduleRetryOnTransientFailure() {
		SagaInstance saga = givenSagaInProgress();
		givenDefinitionRegistered();
		when(stepExecutor.execute(eq(saga.getId()), eq(STEP_1), any()))
				.thenThrow(new IllegalStateException("banco indisponível"));

		SagaState state = orchestrator.advance(saga.getId());

		assertThat(state).isEqualTo(SagaState.RUNNING);
		assertThat(saga.getAttempts()).isEqualTo(1);
		assertThat(saga.getNextAttemptAt()).isAfter(Instant.now());
		assertThat(saga.getLastError()).contains("banco indisponível");
		verify(metrics).sagaRetried();
		verify(stepExecutor, never()).execute(any(), eq(COMPENSATION_1), any());
	}

	@Test
	void shouldCompensateWhenAttemptsAreExhausted() {
		SagaInstance saga = givenSagaInProgress();
		givenDefinitionRegistered();
		when(stepExecutor.execute(eq(saga.getId()), eq(STEP_1), any()))
				.thenThrow(new IllegalStateException("erro persistente"));

		// o contador de tentativas é por passo: cada ciclo reagenda até esgotar
		SagaState first = orchestrator.advance(saga.getId());
		SagaState second = orchestrator.advance(saga.getId());
		SagaState third = orchestrator.advance(saga.getId());

		assertThat(first).isEqualTo(SagaState.RUNNING);
		assertThat(second).isEqualTo(SagaState.RUNNING);
		assertThat(third).isEqualTo(SagaState.COMPENSATED);
		verify(metrics, times(2)).sagaRetried();
		// nada foi executado, então não há o que compensar — só o desfecho negativo
		verify(stepExecutor, never()).execute(any(), eq(COMPENSATION_1), any());
		verify(stepExecutor).execute(saga.getId(), FAILURE_STEP, new TestCommand("fail"));
	}

	@Test
	void shouldFailSagaWhenCompensationIsNotPossible() {
		SagaInstance saga = givenSagaInProgress();
		givenDefinitionRegistered();
		ReflectionTestUtils.setField(saga, "stepIndex", 2);
		saga.startCompensation("falha no crédito");
		when(stepExecutor.execute(eq(saga.getId()), eq(COMPENSATION_1), any()))
				.thenThrow(new TransferRejectedException("conta de origem bloqueada"));

		SagaState state = orchestrator.advance(saga.getId());

		assertThat(state).isEqualTo(SagaState.FAILED);
		assertThat(saga.getLastError()).contains("Falha ao compensar");
		verify(metrics).sagaFailed();
	}

	@Test
	void shouldNotAdvanceTerminalSaga() {
		SagaInstance saga = givenSagaInProgress();
		saga.complete();

		SagaState state = orchestrator.advance(saga.getId());

		assertThat(state).isEqualTo(SagaState.COMPLETED);
		verify(stepExecutor, never()).execute(any(), anyString(), any());
	}

	private SagaInstance givenSagaInProgress() {
		SagaInstance saga = SagaInstance.start(SAGA_TYPE, "{\"value\":\"v\"}", "key-1", "corr-1", Instant.now());
		ReflectionTestUtils.setField(saga, "id", UUID.randomUUID());
		when(repository.findById(saga.getId())).thenReturn(Optional.of(saga));
		return saga;
	}

	/** Registra a definição da saga (necessário em todo avanço não terminal). */
	private void givenDefinitionRegistered() {
		doReturn(definition).when(registry).definition(SAGA_TYPE);
	}

	record TestContext(String value) {
	}

	record TestCommand(String name) {
	}

}
