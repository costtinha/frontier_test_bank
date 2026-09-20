package com.frontier.bank.common.event;

import java.time.Instant;
import java.util.UUID;

import tools.jackson.databind.JsonNode;

/**
 * Formato de fio do evento: o envelope + payload já como JSON.
 * É o que o transporte (Kafka, HTTP, log) entrega aos consumidores.
 */
public record EventMessage(
		UUID eventId,
		String eventType,
		int eventVersion,
		String aggregateType,
		UUID aggregateId,
		Instant occurredAt,
		String correlationId,
		String causationId,
		String actorId,
		JsonNode payload) {
}
