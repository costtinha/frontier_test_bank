package com.frontier.bank.common.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Relay do outbox: acorda periodicamente e delega a publicação ao
 * {@link OutboxDispatcher}.
 * <p>
 * A anotação {@code @Scheduled} fica neste bean justamente para que a chamada
 * atravesse o proxy transacional do dispatcher — auto-invocação (método
 * {@code @Transactional} chamado de dentro da mesma classe) anularia a transação.
 */
@Component
public class OutboxRelay {

	private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

	private final OutboxDispatcher dispatcher;
	private final int batchSize;

	public OutboxRelay(OutboxDispatcher dispatcher,
			@Value("${bank.events.outbox.batch-size:50}") int batchSize) {
		this.dispatcher = dispatcher;
		this.batchSize = batchSize;
	}

	@Scheduled(fixedDelayString = "${bank.events.outbox.poll-interval:2000}")
	public void publishPendingEvents() {
		try {
			int published = dispatcher.publishPending(batchSize);
			if (published > 0) {
				log.debug("Relay publicou {} evento(s) do outbox", published);
			}
		} catch (RuntimeException e) {
			// falha inesperada (ex.: banco indisponível): não deixa o scheduler morrer
			log.error("Falha ao processar o outbox: {}", e.getMessage(), e);
		}
	}

}
