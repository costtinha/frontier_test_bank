package com.frontier.bank.common.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Transporte padrão de desenvolvimento: apenas registra o evento no log,
 * permitindo validar todo o fluxo (outbox → relay → publicação) sem broker.
 */
@Component
@ConditionalOnProperty(name = "bank.events.transport", havingValue = "logging", matchIfMissing = true)
public class LoggingEventTransport implements EventTransport {

	private static final Logger log = LoggerFactory.getLogger(LoggingEventTransport.class);

	@Override
	public void publish(EventMessage message) {
		log.info("Evento publicado [transport=logging] type={} version={} aggregate={}/{} eventId={} correlationId={} payload={}",
				message.eventType(), message.eventVersion(), message.aggregateType(), message.aggregateId(),
				message.eventId(), message.correlationId(), message.payload());
	}

}
