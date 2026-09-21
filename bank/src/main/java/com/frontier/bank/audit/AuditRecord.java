package com.frontier.bank.audit;

import java.time.Instant;
import java.util.UUID;

import com.frontier.bank.common.event.DomainEvent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Registro imutável da trilha de auditoria (WORM).
 * <p>
 * Não há setters: depois de gravado, o registro nunca é alterado — o banco
 * também rejeita UPDATE/DELETE na tabela (trigger).
 */
@Entity
@Table(name = "audit_records")
public class AuditRecord {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

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

	@Column(name = "actor_id", length = 64)
	private String actorId;

	@Column(name = "correlation_id", length = 64)
	private String correlationId;

	@Column(name = "causation_id", length = 64)
	private String causationId;

	@Column(name = "occurred_at", nullable = false)
	private Instant occurredAt;

	@Column(name = "recorded_at", nullable = false)
	private Instant recordedAt;

	@Column(name = "payload", nullable = false, length = 65535)
	private String payload;

	@Column(name = "previous_hash", nullable = false, length = 64)
	private String previousHash;

	@Column(name = "record_hash", nullable = false, length = 64)
	private String recordHash;

	protected AuditRecord() {
		// construtor exigido pelo JPA
	}

	/** Monta o registro e calcula seu hash encadeado ao anterior. */
	public static AuditRecord of(DomainEvent<?> event, String payloadJson, String previousHash) {
		AuditRecord record = new AuditRecord();
		record.eventId = event.eventId();
		record.eventType = event.eventType();
		record.eventVersion = event.eventVersion();
		record.aggregateType = event.aggregateType();
		record.aggregateId = event.aggregateId();
		record.actorId = event.actorId();
		record.correlationId = event.correlationId();
		record.causationId = event.causationId();
		record.occurredAt = event.occurredAt();
		record.recordedAt = Instant.now();
		record.payload = payloadJson;
		record.previousHash = previousHash;
		record.recordHash = AuditHasher.hash(previousHash, record);
		return record;
	}

	public Long getId() {
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

	public String getActorId() {
		return actorId;
	}

	public String getCorrelationId() {
		return correlationId;
	}

	public String getCausationId() {
		return causationId;
	}

	public Instant getOccurredAt() {
		return occurredAt;
	}

	public Instant getRecordedAt() {
		return recordedAt;
	}

	public String getPayload() {
		return payload;
	}

	public String getPreviousHash() {
		return previousHash;
	}

	public String getRecordHash() {
		return recordHash;
	}

}
