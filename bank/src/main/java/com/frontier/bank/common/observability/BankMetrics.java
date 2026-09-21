package com.frontier.bank.common.observability;

import org.springframework.stereotype.Component;

import com.frontier.bank.common.event.OutboxEventRepository;
import com.frontier.bank.common.projection.ProjectionDeadLetterRepository;

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

	public BankMetrics(MeterRegistry registry, OutboxEventRepository outboxRepository,
			ProjectionDeadLetterRepository deadLetterRepository) {
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

		gauge(registry, "bank.outbox.pending", "Eventos ainda não publicados na outbox",
				outboxRepository, OutboxEventRepository::countByPublishedAtIsNull);
		gauge(registry, "bank.projection.dead_letters", "Eventos em dead letter aguardando intervenção",
				deadLetterRepository, ProjectionDeadLetterRepository::count);
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

}
