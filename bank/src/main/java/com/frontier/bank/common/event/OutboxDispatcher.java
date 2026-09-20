package com.frontier.bank.common.event;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Publica lotes de eventos pendentes da outbox.
 * <p>
 * Uma transação por lote: as linhas ficam bloqueadas ({@code FOR UPDATE SKIP LOCKED})
 * durante a publicação e as publicadas são marcadas antes do commit. Se a
 * aplicação cair depois de publicar e antes de commitar, a linha volta a ficar
 * pendente e o evento é reenviado — daí a exigência de consumidores idempotentes
 * (dedupe por {@code eventId}).
 */
@Component
public class OutboxDispatcher {

	private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);

	private final OutboxEventRepository repository;
	private final EventTransport transport;
	private final ObjectMapper objectMapper;

	public OutboxDispatcher(OutboxEventRepository repository, EventTransport transport, ObjectMapper objectMapper) {
		this.repository = repository;
		this.transport = transport;
		this.objectMapper = objectMapper;
	}

	/**
	 * @return quantos eventos foram publicados com sucesso neste lote
	 */
	@Transactional
	public int publishPending(int batchSize) {
		List<OutboxEvent> batch = repository.lockPendingBatch(batchSize);
		int published = 0;

		for (OutboxEvent row : batch) {
			try {
				transport.publish(toMessage(row));
				row.markPublished(Instant.now());
				published++;
			} catch (RuntimeException e) {
				// falha isolada: o evento continua pendente para a próxima tentativa,
				// sem derrubar o restante do lote
				row.markFailed(e.getMessage());
				log.warn("Falha ao publicar evento {} tipo={} (tentativa {}): {}",
						row.getEventId(), row.getEventType(), row.getAttempts(), e.getMessage());
			}
		}
		// as linhas são entidades gerenciadas: o flush no commit persiste
		// published_at/attempts/last_error via dirty checking
		return published;
	}

	private EventMessage toMessage(OutboxEvent row) {
		return new EventMessage(
				row.getEventId(),
				row.getEventType(),
				row.getEventVersion(),
				row.getAggregateType(),
				row.getAggregateId(),
				row.getOccurredAt(),
				row.getCorrelationId(),
				row.getCausationId(),
				row.getActorId(),
				readPayload(row));
	}

	private JsonNode readPayload(OutboxEvent row) {
		try {
			return objectMapper.readTree(row.getPayload());
		} catch (JacksonException e) {
			throw new EventSerializationException("Payload inválido no evento " + row.getEventId(), e);
		}
	}

}
