package com.frontier.bank.saga;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Registro de execução de um passo (ou de sua compensação).
 * <p>
 * A chave única {@code (sagaId, stepName)} é o que dá idempotência aos passos:
 * um retry — ou duas instâncias avançando a mesma saga — nunca aplica o mesmo
 * passo duas vezes, porque o registro é gravado na mesma transação dos efeitos
 * do passo.
 */
@Entity
@Table(name = "saga_step_executions")
public class SagaStepExecution {

	@Id
	private UUID id;

	@Column(name = "saga_id", nullable = false)
	private UUID sagaId;

	@Column(name = "step_name", nullable = false, length = 80)
	private String stepName;

	@Column(name = "executed_at", nullable = false)
	private Instant executedAt;

	protected SagaStepExecution() {
		// construtor exigido pelo JPA
	}

	public static SagaStepExecution of(UUID sagaId, String stepName) {
		SagaStepExecution execution = new SagaStepExecution();
		execution.id = UUID.randomUUID();
		execution.sagaId = sagaId;
		execution.stepName = stepName;
		execution.executedAt = Instant.now();
		return execution;
	}

	public UUID getId() {
		return id;
	}

	public UUID getSagaId() {
		return sagaId;
	}

	public String getStepName() {
		return stepName;
	}

	public Instant getExecutedAt() {
		return executedAt;
	}

}
