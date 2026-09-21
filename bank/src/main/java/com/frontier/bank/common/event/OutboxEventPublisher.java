package com.frontier.bank.common.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.frontier.bank.common.event.schema.EventSchemaRegistry;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Implementação transacional: valida o contrato, serializa o payload, grava a
 * linha na outbox e publica o evento in-process.
 * <p>
 * {@code Propagation.MANDATORY} é intencional: garante que o evento e a mudança
 * de estado entram no banco na mesma transação. Chamar isso fora de uma
 * transação falha rápido, em vez de produzir evento em estado indeterminado.
 * <p>
 * O evento só é aceito se respeitar o schema registrado — um payload fora do
 * contrato derruba a transação e nada é publicado.
 */
@Component
public class OutboxEventPublisher implements EventPublisher {

	private final OutboxEventRepository repository;
	private final EventSchemaRegistry schemaRegistry;
	private final ObjectMapper objectMapper;
	private final ApplicationEventPublisher applicationEventPublisher;

	public OutboxEventPublisher(OutboxEventRepository repository, EventSchemaRegistry schemaRegistry,
			ObjectMapper objectMapper, ApplicationEventPublisher applicationEventPublisher) {
		this.repository = repository;
		this.schemaRegistry = schemaRegistry;
		this.objectMapper = objectMapper;
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	@Transactional(propagation = Propagation.MANDATORY)
	public void publish(DomainEvent<?> event) {
		schemaRegistry.validate(event);
		repository.save(OutboxEvent.from(event, serialize(event)));
		// listeners in-process (@TransactionalEventListener AFTER_COMMIT) recebem
		// somente depois do commit — nunca veem estado não persistido
		applicationEventPublisher.publishEvent(event);
	}

	private String serialize(DomainEvent<?> event) {
		try {
			return objectMapper.writeValueAsString(event.payload());
		} catch (JacksonException e) {
			throw new EventSerializationException("Falha ao serializar o payload do evento " + event.eventType(), e);
		}
	}

}
