package com.frontier.bank.common.projection;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Posição de um consumidor no log de eventos — o "offset" da projeção.
 * <p>
 * Avança sempre na mesma transação em que a projeção foi aplicada, de modo que
 * o checkpoint nunca fica à frente do read model.
 */
@Entity
@Table(name = "projection_checkpoints")
public class ProjectionCheckpoint {

	/** Início do log (usado por consumidores criados sem bootstrap). */
	public static final Instant EPOCH = Instant.EPOCH;
	public static final UUID NO_EVENT = new UUID(0L, 0L);

	@Id
	@Column(name = "consumer", length = 60)
	private String consumer;

	@Column(name = "last_occurred_at", nullable = false)
	private Instant lastOccurredAt;

	@Column(name = "last_event_id", nullable = false)
	private UUID lastEventId;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected ProjectionCheckpoint() {
		// construtor exigido pelo JPA
	}

	/**
	 * Checkpoint inicial: consome o log desde o começo (reconstrói o read model
	 * a partir do histórico completo).
	 */
	public static ProjectionCheckpoint initial(String consumer) {
		ProjectionCheckpoint checkpoint = new ProjectionCheckpoint();
		checkpoint.consumer = consumer;
		checkpoint.lastOccurredAt = EPOCH;
		checkpoint.lastEventId = NO_EVENT;
		checkpoint.updatedAt = Instant.now();
		return checkpoint;
	}

	public void advanceTo(Instant occurredAt, UUID eventId) {
		this.lastOccurredAt = occurredAt;
		this.lastEventId = eventId;
		this.updatedAt = Instant.now();
	}

	public String getConsumer() {
		return consumer;
	}

	public Instant getLastOccurredAt() {
		return lastOccurredAt;
	}

	public UUID getLastEventId() {
		return lastEventId;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
