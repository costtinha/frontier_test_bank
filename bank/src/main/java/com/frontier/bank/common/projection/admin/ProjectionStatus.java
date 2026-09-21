package com.frontier.bank.common.projection.admin;

import java.time.Instant;
import java.util.UUID;

/**
 * Situação de um consumidor de read model: onde parou, quanto já processou,
 * quanto ainda falta e quantos eventos estão em dead letter.
 */
public record ProjectionStatus(
		String consumer,
		Instant lastOccurredAt,
		UUID lastEventId,
		long processedEvents,
		long pendingEvents,
		long deadLetters) {
}
