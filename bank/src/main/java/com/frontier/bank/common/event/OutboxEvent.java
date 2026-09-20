package com.frontier.bank.common.event;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Linha da tabela outbox: o evento persistido <b>atomicamente</b> com a mudança
 * de estado que o originou.
 * <p>
 * É infraestrutura, não um agregado de domínio — por isso não tem repositório
 * exposto ao lado de escrita dos agregados.
 */
@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

	private static final int MAX_ERROR_LENGTH = 500;

	@Id
	private UUID id;

	@Column(name = "event_id", nullable = false, unique = true)
	private UUID eventId;

	@Column(name = "event_type", nullable = false, length = 120)
	private String eventType;

	@Column(name = "event_version", nullable = false)
	private int eventVersion;

	@Column(name = "aggregate_type", nullable = false, length = 60)
	private String aggregateType;

	@Column(name = "aggregate_id", nullable = false)
	private UUID aggregateId;

	@Column(name = "correlation_id", length = 64)
	private String correlationId;

	@Column(name = "causation_id", length = 64)
	private String causationId;

	@Column(name = "actor_id", length = 64)
	private String actorId;

	@Column(name = "occurred_at", nullable = false)
	private Instant occurredAt;

	/** Payload serializado em JSON (a ser migrado para jsonb quando houver consulta direta). */
	@Column(name = "payload", nullable = false, length = 65535)
	private String payload;

	@Column(name = "published_at")
	private Instant publishedAt;

	@Column(name = "attempts", nullable = false)
	private int attempts;

	@Column(name = "last_error", length = MAX_ERROR_LENGTH)
	private String lastError;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected OutboxEvent() {
		// construtor exigido pelo JPA
	}

	public static OutboxEvent from(DomainEvent<?> event, String payloadJson) {
		OutboxEvent row = new OutboxEvent();
		row.id = UUID.randomUUID();
		row.eventId = event.eventId();
		row.eventType = event.eventType();
		row.eventVersion = event.eventVersion();
		row.aggregateType = event.aggregateType();
		row.aggregateId = event.aggregateId();
		row.correlationId = event.correlationId();
		row.causationId = event.causationId();
		row.actorId = event.actorId();
		row.occurredAt = event.occurredAt();
		row.payload = payloadJson;
		row.attempts = 0;
		row.createdAt = Instant.now();
		return row;
	}

	void markPublished(Instant when) {
		this.publishedAt = when;
		this.lastError = null;
		this.attempts++;
	}

	void markFailed(String error) {
		this.attempts++;
		this.lastError = error == null
				? null
				: error.substring(0, Math.min(error.length(), MAX_ERROR_LENGTH));
	}

	public UUID getId() {
		return id;
	}

	public UUID getEventId() {
		return eventId;
	}

	public String getEventType() {
		return eventType;
	}

	public int getEventVersion() {
		return eventVersion;
	}

	public String getAggregateType() {
		return aggregateType;
	}

	public UUID getAggregateId() {
		return aggregateId;
	}

	public String getCorrelationId() {
		return correlationId;
	}

	public String getCausationId() {
		return causationId;
	}

	public String getActorId() {
		return actorId;
	}

	public Instant getOccurredAt() {
		return occurredAt;
	}

	public String getPayload() {
		return payload;
	}

	public Instant getPublishedAt() {
		return publishedAt;
	}

	public int getAttempts() {
		return attempts;
	}

	public String getLastError() {
		return lastError;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
