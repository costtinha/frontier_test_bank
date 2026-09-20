package com.frontier.bank.common.projection;

import java.time.Instant;
import java.util.UUID;

import com.frontier.bank.common.event.EventMessage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Dead letter de projeção: um evento que falhou ao ser aplicado.
 * <p>
 * Enquanto as tentativas forem menores que o limite configurado, o evento é
 * reprocessado nos ciclos seguintes. Ao esgotar, ele é pulado (o checkpoint
 * avança) mas permanece registrado aqui — nada é perdido silenciosamente.
 */
@Entity
@Table(name = "projection_dead_letters")
public class ProjectionDeadLetter {

	private static final int MAX_ERROR_LENGTH = 1000;

	@Id
	private UUID id;

	@Column(name = "consumer", nullable = false, length = 60)
	private String consumer;

	@Column(name = "event_id", nullable = false)
	private UUID eventId;

	@Column(name = "event_type", nullable = false, length = 120)
	private String eventType;

	@Column(name = "aggregate_id", nullable = false)
	private UUID aggregateId;

	@Column(name = "payload", nullable = false, length = 65535)
	private String payload;

	@Column(name = "error", length = MAX_ERROR_LENGTH)
	private String error;

	@Column(name = "attempts", nullable = false)
	private int attempts;

	@Column(name = "first_failed_at", nullable = false)
	private Instant firstFailedAt;

	@Column(name = "last_failed_at", nullable = false)
	private Instant lastFailedAt;

	protected ProjectionDeadLetter() {
		// construtor exigido pelo JPA
	}

	public static ProjectionDeadLetter first(String consumer, EventMessage event, String error) {
		ProjectionDeadLetter deadLetter = new ProjectionDeadLetter();
		deadLetter.id = UUID.randomUUID();
		deadLetter.consumer = consumer;
		deadLetter.eventId = event.eventId();
		deadLetter.eventType = event.eventType();
		deadLetter.aggregateId = event.aggregateId();
		deadLetter.payload = event.payload() == null ? "{}" : event.payload().toString();
		deadLetter.error = truncate(error);
		deadLetter.attempts = 1;
		deadLetter.firstFailedAt = Instant.now();
		deadLetter.lastFailedAt = deadLetter.firstFailedAt;
		return deadLetter;
	}

	public void failedAgain(String error) {
		this.attempts++;
		this.error = truncate(error);
		this.lastFailedAt = Instant.now();
	}

	public boolean isExhausted(int maxAttempts) {
		return this.attempts >= maxAttempts;
	}

	private static String truncate(String error) {
		if (error == null) {
			return null;
		}
		return error.substring(0, Math.min(error.length(), MAX_ERROR_LENGTH));
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

	public UUID getAggregateId() {
		return aggregateId;
	}

	public String getPayload() {
		return payload;
	}

	public String getError() {
		return error;
	}

	public int getAttempts() {
		return attempts;
	}

	public Instant getFirstFailedAt() {
		return firstFailedAt;
	}

	public Instant getLastFailedAt() {
		return lastFailedAt;
	}

}
