package com.frontier.bank.saga.admin;

import java.time.Instant;
import java.util.UUID;

import com.frontier.bank.saga.SagaState;

/**
 * Visão operacional de uma saga: onde está, quanto falta, quando será retomada e
 * por que falhou.
 */
public record SagaStatus(
		UUID id,
		String sagaType,
		SagaState state,
		int completedSteps,
		int totalSteps,
		int attempts,
		Instant nextAttemptAt,
		String lastError,
		String correlationId,
		Instant createdAt,
		Instant updatedAt) {

	public boolean awaitingIntervention() {
		return state == SagaState.FAILED;
	}

}
