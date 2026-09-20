package com.frontier.bank.common.event;

import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Converte eventos para o formato de fio ({@link EventMessage}).
 * <p>
 * Dois caminhos: da linha persistida na outbox (payload já em JSON) e do evento
 * in-process (payload tipado, serializado sob demanda). Consumidores — relay de
 * publicação, projeções — trabalham sempre com o mesmo contrato.
 */
@Component
public class EventMessageMapper {

	private final ObjectMapper objectMapper;

	public EventMessageMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	/** A partir da linha da outbox. */
	public EventMessage toMessage(OutboxEvent row) {
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

	/** A partir do evento in-process (publicado na mesma transação). */
	public EventMessage toMessage(DomainEvent<?> event) {
		return new EventMessage(
				event.eventId(),
				event.eventType(),
				event.eventVersion(),
				event.aggregateType(),
				event.aggregateId(),
				event.occurredAt(),
				event.correlationId(),
				event.causationId(),
				event.actorId(),
				objectMapper.valueToTree(event.payload()));
	}

	private JsonNode readPayload(OutboxEvent row) {
		try {
			return objectMapper.readTree(row.getPayload());
		} catch (JacksonException e) {
			throw new EventSerializationException("Payload inválido no evento " + row.getEventId(), e);
		}
	}

}
