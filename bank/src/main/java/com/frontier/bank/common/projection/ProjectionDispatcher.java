package com.frontier.bank.common.projection;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.frontier.bank.common.context.CorrelationContext;
import com.frontier.bank.common.event.EventMessage;
import com.frontier.bank.common.event.EventMessageMapper;
import com.frontier.bank.common.event.OutboxEvent;
import com.frontier.bank.common.event.OutboxEventRepository;
import com.frontier.bank.common.observability.BankMetrics;

/**
 * Orquestra os consumidores de read model: para cada {@link ProjectionHandler},
 * lê o lote de eventos seguinte ao checkpoint e o aplica em ordem.
 * <p>
 * Política de falha:
 * <ul>
 *   <li>evento irrelevante para o consumidor → pula (avança a posição);</li>
 *   <li>falha ao aplicar → registra na dead letter e <b>para o lote</b>: a
 *       posição não avança, então o evento é reprocessado no próximo ciclo
 *       (bloqueio limitado pelo número máximo de tentativas);</li>
 *   <li>evento com tentativas esgotadas → pula (a dead letter preserva a
 *       evidência) para não travar o stream indefinidamente.</li>
 * </ul>
 */
@Component
public class ProjectionDispatcher {

	private static final Logger log = LoggerFactory.getLogger(ProjectionDispatcher.class);

	private final List<ProjectionHandler> handlers;
	private final OutboxEventRepository outboxRepository;
	private final EventMessageMapper messageMapper;
	private final ProjectionApplier applier;
	private final ProjectionCheckpointRepository checkpointRepository;
	private final ProjectionDeadLetterRepository deadLetterRepository;
	private final BankMetrics metrics;
	private final int maxAttempts;

	public ProjectionDispatcher(List<ProjectionHandler> handlers, OutboxEventRepository outboxRepository,
			EventMessageMapper messageMapper, ProjectionApplier applier,
			ProjectionCheckpointRepository checkpointRepository,
			ProjectionDeadLetterRepository deadLetterRepository, BankMetrics metrics,
			@Value("${bank.projection.max-attempts:5}") int maxAttempts) {
		this.handlers = handlers.stream()
				.sorted(Comparator.comparing(ProjectionHandler::consumerName))
				.toList();
		this.outboxRepository = outboxRepository;
		this.messageMapper = messageMapper;
		this.applier = applier;
		this.checkpointRepository = checkpointRepository;
		this.deadLetterRepository = deadLetterRepository;
		this.metrics = metrics;
		this.maxAttempts = maxAttempts;
	}

	/**
	 * @return quantos eventos foram projetados com sucesso neste ciclo
	 */
	public int processPending(int batchSize) {
		int projected = 0;
		for (ProjectionHandler handler : handlers) {
			projected += processHandler(handler, batchSize);
		}
		return projected;
	}

	private int processHandler(ProjectionHandler handler, int batchSize) {
		ProjectionCheckpoint checkpoint = checkpointRepository.findById(handler.consumerName())
				.orElseGet(() -> ProjectionCheckpoint.initial(handler.consumerName()));

		List<OutboxEvent> batch = outboxRepository.findAfter(
				checkpoint.getLastOccurredAt(), checkpoint.getLastEventId(), batchSize);

		int projected = 0;
		for (OutboxEvent row : batch) {
			EventMessage event = messageMapper.toMessage(row);

			// log correlacionado com a requisição que originou o evento
			CorrelationContext.set(event.correlationId() == null ? event.eventId().toString() : event.correlationId());
			try {
				if (!handler.supportedEventTypes().contains(event.eventType()) || isExhausted(handler, event)) {
					applier.skip(handler, event);
					continue;
				}
				try {
					if (applier.apply(handler, event)) {
						projected++;
						metrics.projectionApplied();
					}
				} catch (RuntimeException e) {
					recordFailure(handler, event, e);
					break; // mantém a posição: reprocessa no próximo ciclo
				}
			} finally {
				CorrelationContext.clear();
			}
		}
		return projected;
	}

	private boolean isExhausted(ProjectionHandler handler, EventMessage event) {
		return deadLetterRepository
				.findByConsumerAndEventId(handler.consumerName(), event.eventId())
				.filter(deadLetter -> deadLetter.isExhausted(maxAttempts))
				.isPresent();
	}

	private void recordFailure(ProjectionHandler handler, EventMessage event, RuntimeException error) {
		String consumer = handler.consumerName();
		Optional<ProjectionDeadLetter> existing =
				deadLetterRepository.findByConsumerAndEventId(consumer, event.eventId());

		ProjectionDeadLetter deadLetter = existing
				.map(record -> {
					record.failedAgain(error.getMessage());
					return record;
				})
				.orElseGet(() -> ProjectionDeadLetter.first(consumer, event, error.getMessage()));

		// gravado fora da transação da projeção (que sofreu rollback)
		deadLetterRepository.save(deadLetter);
		metrics.projectionFailed();
		if (deadLetter.isExhausted(maxAttempts)) {
			metrics.projectionDeadLettered();
		}

		log.warn("Falha ao projetar evento {} ({}) para o consumidor {} — tentativa {}/{}: {}",
				event.eventId(), event.eventType(), consumer,
				deadLetter.getAttempts(), maxAttempts, error.getMessage());
	}

}
