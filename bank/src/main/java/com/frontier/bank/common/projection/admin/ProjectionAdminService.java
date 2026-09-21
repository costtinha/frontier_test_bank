package com.frontier.bank.common.projection.admin;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.frontier.bank.common.error.ResourceNotFoundException;
import com.frontier.bank.common.event.OutboxEvent;
import com.frontier.bank.common.event.OutboxEventRepository;
import com.frontier.bank.common.projection.ProcessedEventRepository;
import com.frontier.bank.common.projection.ProjectionCheckpoint;
import com.frontier.bank.common.projection.ProjectionCheckpointRepository;
import com.frontier.bank.common.projection.ProjectionDeadLetter;
import com.frontier.bank.common.projection.ProjectionDeadLetterRepository;
import com.frontier.bank.common.projection.ProjectionHandler;

/**
 * Operação das projeções: inspeção de estado, <b>replay</b> (rebobinar o
 * checkpoint para reprocessar o log) e reexecução de dead letters.
 * <p>
 * O replay é seguro porque a projeção é idempotente (dedupe por
 * {@code processed_events}) e o projector ignora eventos mais antigos que o
 * estado atual — reprocessar não corrompe o read model.
 */
@Service
public class ProjectionAdminService {

	private static final Logger log = LoggerFactory.getLogger(ProjectionAdminService.class);

	private final List<ProjectionHandler> handlers;
	private final ProjectionCheckpointRepository checkpointRepository;
	private final ProcessedEventRepository processedEventRepository;
	private final ProjectionDeadLetterRepository deadLetterRepository;
	private final OutboxEventRepository outboxRepository;

	public ProjectionAdminService(List<ProjectionHandler> handlers,
			ProjectionCheckpointRepository checkpointRepository,
			ProcessedEventRepository processedEventRepository,
			ProjectionDeadLetterRepository deadLetterRepository,
			OutboxEventRepository outboxRepository) {
		this.handlers = handlers;
		this.checkpointRepository = checkpointRepository;
		this.processedEventRepository = processedEventRepository;
		this.deadLetterRepository = deadLetterRepository;
		this.outboxRepository = outboxRepository;
	}

	@Transactional(readOnly = true)
	public List<ProjectionStatus> status() {
		List<ProjectionStatus> statuses = new ArrayList<>();
		for (ProjectionHandler handler : handlers) {
			ProjectionCheckpoint checkpoint = checkpointRepository.findById(handler.consumerName())
					.orElseGet(() -> ProjectionCheckpoint.initial(handler.consumerName()));

			statuses.add(new ProjectionStatus(
					handler.consumerName(),
					checkpoint.getLastOccurredAt(),
					checkpoint.getLastEventId(),
					processedEventRepository.countByConsumer(handler.consumerName()),
					outboxRepository.countAfter(checkpoint.getLastOccurredAt(), checkpoint.getLastEventId()),
					deadLetterRepository.countByConsumer(handler.consumerName())));
		}
		return statuses;
	}

	/**
	 * Rebobina o checkpoint do consumidor e limpa o registro de dedupe, fazendo o
	 * relay reprocessar o log a partir de {@code from} (ou desde o início).
	 *
	 * @param from instante a partir do qual reprocessar; {@code null} = desde o início do log
	 */
	@Transactional
	public ProjectionStatus replay(String consumer, Instant from) {
		requireHandler(consumer);

		Instant start = from == null ? ProjectionCheckpoint.EPOCH : from;
		ProjectionCheckpoint checkpoint = ProjectionCheckpoint.initial(consumer);
		checkpoint.advanceTo(start, ProjectionCheckpoint.NO_EVENT);
		checkpointRepository.save(checkpoint);

		processedEventRepository.deleteByConsumer(consumer);

		log.info("Replay solicitado para o consumidor {} a partir de {}", consumer, start);
		return statusOf(consumer);
	}

	/**
	 * Reexecuta um evento que ficou na dead letter: limpa o registro de falha e
	 * rebobina o checkpoint até o evento, para que ele seja reprojetado no
	 * próximo ciclo do relay.
	 */
	@Transactional
	public ProjectionStatus retryDeadLetter(String consumer, UUID eventId) {
		requireHandler(consumer);

		ProjectionDeadLetter deadLetter = deadLetterRepository.findByConsumerAndEventId(consumer, eventId)
				.orElseThrow(() -> new ResourceNotFoundException("evento em dead letter", eventId));
		OutboxEvent event = outboxRepository.findByEventId(deadLetter.getEventId())
				.orElseThrow(() -> new ResourceNotFoundException("evento no log", deadLetter.getEventId()));

		deadLetterRepository.delete(deadLetter);

		// o consumo é inclusivo no checkpoint, então posicionar nele basta para reprocessar
		ProjectionCheckpoint checkpoint = checkpointRepository.findById(consumer)
				.orElseGet(() -> ProjectionCheckpoint.initial(consumer));
		checkpoint.advanceTo(event.getOccurredAt(), event.getEventId());
		checkpointRepository.save(checkpoint);

		log.info("Dead letter reenfileirada: consumidor={} eventId={}", consumer, eventId);
		return statusOf(consumer);
	}

	private ProjectionStatus statusOf(String consumer) {
		return status().stream()
				.filter(status -> status.consumer().equals(consumer))
				.findFirst()
				.orElseThrow(() -> new ResourceNotFoundException("consumidor de projeção", consumer));
	}

	private void requireHandler(String consumer) {
		boolean known = handlers.stream().anyMatch(handler -> handler.consumerName().equals(consumer));
		if (!known) {
			throw new ResourceNotFoundException("consumidor de projeção", consumer);
		}
	}

}
