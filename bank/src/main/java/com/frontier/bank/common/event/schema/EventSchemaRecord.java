package com.frontier.bank.common.event.schema;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Schema vigente de um tipo de evento (linha por tipo).
 */
@Entity
@Table(name = "event_schemas")
public class EventSchemaRecord {

	@Id
	@Column(name = "event_type", length = 120)
	private String eventType;

	@Column(nullable = false)
	private int version;

	@Column(nullable = false, length = 64)
	private String fingerprint;

	@Column(nullable = false, length = 4000)
	private String fields;

	@Column(name = "registered_at", nullable = false)
	private Instant registeredAt;

	protected EventSchemaRecord() {
		// construtor exigido pelo JPA
	}

	public static EventSchemaRecord of(EventSchema schema) {
		EventSchemaRecord record = new EventSchemaRecord();
		record.eventType = schema.eventType();
		record.updateTo(schema);
		return record;
	}

	public void updateTo(EventSchema schema) {
		this.version = schema.version();
		this.fingerprint = schema.fingerprint();
		this.fields = schema.serializedFields();
		this.registeredAt = Instant.now();
	}

	public EventSchema toSchema() {
		return EventSchema.fromSerialized(eventType, version, fields);
	}

	public String getEventType() {
		return eventType;
	}

	public int getVersion() {
		return version;
	}

	public String getFingerprint() {
		return fingerprint;
	}

	public String getFields() {
		return fields;
	}

	public Instant getRegisteredAt() {
		return registeredAt;
	}

}
