package com.frontier.bank.common.projection;

import java.time.Instant;
import java.util.UUID;

import com.frontier.bank.common.event.EventMessage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Registro de que um evento já foi aplicado por um consumidor.
 * <p>
 * É o que torna a projeção idempotente sob entrega at-least-once (replay do log,
 * redelivery do broker na Fase 3): o mesmo evento pode chegar mais de uma vez,
 * mas é aplicado uma única vez por consumidor.
 */
@Entity
@Table(name = "processed_events")
public class ProcessedEvent {

	@Id
	private UUID id;

	@Column(name = "consumer", nullable = false, length = 60)
	private String consumer;

	@Column(name = "event_id", nullable = false)
	private UUID eventId;

	@Column(name = "event_type", nullable = false, length = 120)
	private String eventType;

	@Column(name = "processed_at", nullable = false)
	private Instant processedAt;

	protected ProcessedEvent() {
		// construtor exigido pelo JPA
	}

	public static ProcessedEvent of(String consumer, EventMessage event) {
		ProcessedEvent processed = new ProcessedEvent();
		processed.id = UUID.randomUUID();
		processed.consumer = consumer;
		processed.eventId = event.eventId();
		processed.eventType = event.eventType();
		processed.processedAt = Instant.now();
		return processed;
	}

	public UUID getId() {
		return id;
	}

	public String getConsumer() {
		return consumer;
	}

	public UUID getEventId() {
		return eventId;
	}

	public String getEventType() {
		return eventType;
	}

	public Instant getProcessedAt() {
		return processedAt;
	}

}
