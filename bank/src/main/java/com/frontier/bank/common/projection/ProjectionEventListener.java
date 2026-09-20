package com.frontier.bank.common.projection;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.frontier.bank.common.event.DomainEvent;
import com.frontier.bank.common.event.EventMessage;
import com.frontier.bank.common.event.EventMessageMapper;

/**
 * Projeção em tempo real: aplica o read model logo após o commit da transação
 * de escrita, na própria requisição.
 * <p>
 * Isso garante <b>read-your-writes</b> (o POST de depósito devolve o saldo já
 * atualizado, mesmo com read model eventualmente consistente). Como a aplicação
 * é idempotente, o {@link ProjectionRelay} pode reprocessar o mesmo evento sem
 * efeito colateral — a durabilidade continua garantida pelo log de eventos.
 */
@Component
public class ProjectionEventListener {

	private static final Logger log = LoggerFactory.getLogger(ProjectionEventListener.class);

	private final List<ProjectionHandler> handlers;
	private final ProjectionApplier applier;
	private final EventMessageMapper messageMapper;

	public ProjectionEventListener(List<ProjectionHandler> handlers, ProjectionApplier applier,
			EventMessageMapper messageMapper) {
		this.handlers = handlers;
		this.applier = applier;
		this.messageMapper = messageMapper;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onDomainEvent(DomainEvent<?> event) {
		EventMessage message = messageMapper.toMessage(event);

		for (ProjectionHandler handler : handlers) {
			if (!handler.supportedEventTypes().contains(message.eventType())) {
				continue;
			}
			try {
				applier.apply(handler, message);
			} catch (RuntimeException e) {
				// a falha não derruba a requisição: o relay reagenda e, após
				// esgotar as tentativas, o evento vai para a dead letter
				log.warn("Projeção síncrona falhou (consumidor={}, evento={}): {}",
						handler.consumerName(), message.eventId(), e.getMessage());
			}
		}
	}

}
