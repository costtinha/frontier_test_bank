package com.frontier.bank.common.observability;

import java.util.List;

import org.springframework.stereotype.Component;

import com.frontier.bank.common.event.OutboxEventRepository;
import com.frontier.bank.common.projection.ProjectionDeadLetterRepository;
import com.frontier.bank.saga.SagaInstanceRepository;
import com.frontier.bank.saga.SagaState;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Métricas de negócio e de infraestrutura expostas ao Micrometer (via actuator).
 * <p>
 * Contadores dos caminhos quentes (publicação de eventos, projeções,
 * transferências) e medidores calculados no momento da coleta (backlog do
 * outbox e dead letters pendentes) — os dois indicadores que dizem se o lado
 * assíncrono está acompanhando a escrita.
 */
@Component
public class BankMetrics {

	private final Counter eventsPublished;
	private final Counter eventPublishFailures;
	private final Counter projectionsApplied;
	private final Counter projectionsFailed;
	private final Counter projectionDeadLetters;
	private final Counter transfersCompleted;
	private final Counter transfersRejected;
	private final Counter sagasCompleted;
	private final Counter sagasCompensated;
	private final Counter sagasFailed;
	private final Counter sagasRetried;
	private final Counter sagasRejected;
	private final Counter sagaCompensations;

	public BankMetrics(MeterRegistry registry, OutboxEventRepository outboxRepository,
			ProjectionDeadLetterRepository deadLetterRepository, SagaInstanceRepository sagaRepository) {
		this.eventsPublished = Counter.builder("bank.events.published")
				.description("Eventos de domínio publicados no transporte")
				.register(registry);
		this.eventPublishFailures = Counter.builder("bank.events.publish.failures")
				.description("Falhas de publicação de eventos (permanecem pendentes na outbox)")
				.register(registry);
		this.projectionsApplied = Counter.builder("bank.projection.applied")
				.description("Eventos aplicados em read models")
				.register(registry);
		this.projectionsFailed = Counter.builder("bank.projection.failed")
				.description("Falhas ao aplicar eventos em read models")
				.register(registry);
		this.projectionDeadLetters = Counter.builder("bank.projection.dead.lettered")
				.description("Eventos movidos para a dead letter por esgotar tentativas")
				.register(registry);
		this.transfersCompleted = Counter.builder("bank.transfers.completed")
				.description("Transferências concluídas")
				.register(registry);
		this.transfersRejected = Counter.builder("bank.transfers.rejected")
				.description("Transferências rejeitadas por regra de negócio")
				.register(registry);
		this.sagasCompleted = Counter.builder("bank.saga.completed")
				.description("Sagas concluídas com todos os passos")
				.register(registry);
		this.sagasCompensated = Counter.builder("bank.saga.compensated")
				.description("Sagas compensadas (desfeitas sem efeito residual)")
				.register(registry);
		this.sagasFailed = Counter.builder("bank.saga.failed")
				.description("Sagas que não concluíram nem compensaram — exigem intervenção")
				.register(registry);
		this.sagasRetried = Counter.builder("bank.saga.retried")
				.description("Reagendamentos de saga por falha transitória")
				.register(registry);
		this.sagasRejected = Counter.builder("bank.saga.rejected")
				.description("Sagas rejeitadas por regra de negócio")
				.register(registry);
		this.sagaCompensations = Counter.builder("bank.saga.compensation.executed")
				.description("Passos de compensação executados")
				.register(registry);

		gauge(registry, "bank.outbox.pending", "Eventos ainda não publicados na outbox",
				outboxRepository, OutboxEventRepository::countByPublishedAtIsNull);
		gauge(registry, "bank.projection.dead_letters", "Eventos em dead letter aguardando intervenção",
				deadLetterRepository, ProjectionDeadLetterRepository::count);
		gauge(registry, "bank.saga.awaiting_intervention", "Sagas em FAILED (efeito residual possível)",
				sagaRepository, repository -> repository.countByState(SagaState.FAILED));
		gauge(registry, "bank.saga.running", "Sagas ainda em execução (RUNNING/COMPENSATING)",
				sagaRepository, repository -> repository.countByStateIn(
						List.of(SagaState.RUNNING, SagaState.COMPENSATING)));
	}

	private <T> void gauge(MeterRegistry registry, String name, String description, T source,
			java.util.function.ToDoubleFunction<T> value) {
		Gauge.builder(name, source, value).description(description).register(registry);
	}

	public void eventPublished() {
		eventsPublished.increment();
	}

	public void eventPublishFailed() {
		eventPublishFailures.increment();
	}

	public void projectionApplied() {
		projectionsApplied.increment();
	}

	public void projectionFailed() {
		projectionsFailed.increment();
	}

	public void projectionDeadLettered() {
		projectionDeadLetters.increment();
	}

	public void transferCompleted() {
		transfersCompleted.increment();
	}

	public void transferRejected() {
		transfersRejected.increment();
	}

	public void sagaCompleted() {
		sagasCompleted.increment();
	}

	public void sagaCompensated() {
		sagasCompensated.increment();
	}

	public void sagaFailed() {
		sagasFailed.increment();
	}

	public void sagaRetried() {
		sagasRetried.increment();
	}

	public void sagaRejected() {
		sagasRejected.increment();
	}

	public void sagaCompensationExecuted() {
		sagaCompensations.increment();
	}

}
