package com.frontier.bank.common.event;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
	private final EventMessageMapper messageMapper;

	public OutboxDispatcher(OutboxEventRepository repository, EventTransport transport,
			EventMessageMapper messageMapper) {
		this.repository = repository;
		this.transport = transport;
		this.messageMapper = messageMapper;
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
				transport.publish(messageMapper.toMessage(row));
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

}
