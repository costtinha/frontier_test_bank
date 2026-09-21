package com.frontier.bank.common.projection;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.frontier.bank.common.event.EventMessage;

/**
 * Aplica um evento a um read model em <b>transação própria</b>.
 * <p>
 * Projeção, registro de dedupe e avanço do checkpoint acontecem na mesma
 * transação: ou tudo é commitado, ou nada é — o checkpoint nunca fica à frente
 * do read model. Uma falha derruba apenas este evento, que será reprojetado no
 * próximo ciclo (ou enviado à dead letter após esgotar as tentativas).
 */
@Component
public class ProjectionApplier {

	private final ProjectionCheckpointRepository checkpointRepository;
	private final ProcessedEventRepository processedEventRepository;
	private final ProjectionDeadLetterRepository deadLetterRepository;

	public ProjectionApplier(ProjectionCheckpointRepository checkpointRepository,
			ProcessedEventRepository processedEventRepository,
			ProjectionDeadLetterRepository deadLetterRepository) {
		this.checkpointRepository = checkpointRepository;
		this.processedEventRepository = processedEventRepository;
		this.deadLetterRepository = deadLetterRepository;
	}

	/**
	 * @return {@code true} se a projeção foi executada; {@code false} quando o
	 *         evento já havia sido processado por este consumidor (dedupe)
	 */
	@Transactional
	public boolean apply(ProjectionHandler handler, EventMessage event) {
		String consumer = handler.consumerName();
		boolean projected = false;

		if (!processedEventRepository.existsByConsumerAndEventId(consumer, event.eventId())) {
			handler.apply(event);
			processedEventRepository.save(ProcessedEvent.of(consumer, event));
			projected = true;
		}

		// projeção bem-sucedida limpa falhas anteriores do mesmo evento
		deadLetterRepository.findByConsumerAndEventId(consumer, event.eventId())
				.ifPresent(deadLetterRepository::delete);

		advance(consumer, event);
		return projected;
	}

	/**
	 * Avança a posição sem projetar: usado para eventos irrelevantes para este
	 * consumidor e para eventos já esgotados na dead letter (não bloqueiam o stream).
	 */
	@Transactional
	public void skip(ProjectionHandler handler, EventMessage event) {
		advance(handler.consumerName(), event);
	}

	private void advance(String consumer, EventMessage event) {
		ProjectionCheckpoint checkpoint = checkpointRepository.findById(consumer)
				.orElseGet(() -> ProjectionCheckpoint.initial(consumer));

		// a leitura do log é inclusiva no checkpoint: reler o mesmo evento não deve
		// gerar escrita desnecessária
		if (event.eventId().equals(checkpoint.getLastEventId())) {
			return;
		}
		checkpoint.advanceTo(event.occurredAt(), event.eventId());
		checkpointRepository.save(checkpoint);
	}

}
