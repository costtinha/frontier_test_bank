package com.frontier.bank.saga;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Estado durável de uma saga em execução.
 * <p>
 * É a peça que torna a orquestração resiliente: o avanço (qual passo está em
 * curso), as tentativas e o agendamento de retry vivem no banco, então um
 * reinício da aplicação não perde a saga — o sweeper retoma de onde parou.
 */
@Entity
@Table(name = "saga_instances")
public class SagaInstance {

	@Id
	private UUID id;

	@Column(name = "saga_type", nullable = false, length = 60)
	private String sagaType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SagaState state;

	/** Quantidade de passos já concluídos. */
	@Column(name = "step_index", nullable = false)
	private int stepIndex;

	/** Contexto da saga em JSON (dados necessários para todos os passos). */
	@Column(nullable = false, length = 65535)
	private String payload;

	/** Chave de idempotência da operação de negócio (única quando presente). */
	@Column(name = "idempotency_key", unique = true, length = 80)
	private String idempotencyKey;

	@Column(nullable = false)
	private int attempts;

	@Column(name = "next_attempt_at")
	private Instant nextAttemptAt;

	@Column(name = "last_error", length = 1000)
	private String lastError;

	@Column(name = "correlation_id", length = 64)
	private String correlationId;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	/** Controle otimista: dois avanços concorrentes não se sobrescrevem. */
	@Version
	@Column(nullable = false)
	private long version;

	protected SagaInstance() {
		// construtor exigido pelo JPA
	}

	public static SagaInstance start(String sagaType, String payloadJson, String idempotencyKey,
			String correlationId, Instant firstAttemptAt) {
		SagaInstance saga = new SagaInstance();
		saga.id = UUID.randomUUID();
		saga.sagaType = sagaType;
		saga.state = SagaState.RUNNING;
		saga.stepIndex = 0;
		saga.payload = payloadJson;
		saga.idempotencyKey = idempotencyKey;
		saga.attempts = 0;
		// já nasce elegível para o sweeper: se a execução inline não acontecer
		// (ex.: processo caiu após o commit), a saga é retomada automaticamente
		saga.nextAttemptAt = firstAttemptAt;
		saga.correlationId = correlationId;
		saga.createdAt = Instant.now();
		saga.updatedAt = saga.createdAt;
		return saga;
	}

	public void advanceTo(int stepIndex) {
		this.stepIndex = stepIndex;
		this.attempts = 0;
		this.nextAttemptAt = null;
		this.lastError = null;
		touch();
	}

	public void scheduleRetry(Instant nextAttemptAt, String error) {
		this.attempts++;
		this.nextAttemptAt = nextAttemptAt;
		this.lastError = truncate(error);
		touch();
	}

	public void startCompensation(String reason) {
		this.state = SagaState.COMPENSATING;
		this.lastError = truncate(reason);
		this.attempts = 0;
		this.nextAttemptAt = null;
		touch();
	}

	public void complete() {
		this.state = SagaState.COMPLETED;
		this.lastError = null;
		this.nextAttemptAt = null;
		touch();
	}

	public void compensated() {
		this.state = SagaState.COMPENSATED;
		this.nextAttemptAt = null;
		touch();
	}

	/** Estado de intervenção: não concluiu e não conseguiu desfazer. */
	public void fail(String reason) {
		this.state = SagaState.FAILED;
		this.lastError = truncate(reason);
		this.nextAttemptAt = null;
		touch();
	}

	private void touch() {
		this.updatedAt = Instant.now();
	}

	private static String truncate(String value) {
		if (value == null) {
			return null;
		}
		return value.substring(0, Math.min(value.length(), 1000));
	}

	public boolean isTerminal() {
		return state.isTerminal();
	}

	public UUID getId() {
		return id;
	}

	public String getSagaType() {
		return sagaType;
	}

	public SagaState getState() {
		return state;
	}

	public int getStepIndex() {
		return stepIndex;
	}

	public String getPayload() {
		return payload;
	}

	public String getIdempotencyKey() {
		return idempotencyKey;
	}

	public int getAttempts() {
		return attempts;
	}

	public Instant getNextAttemptAt() {
		return nextAttemptAt;
	}

	public String getLastError() {
		return lastError;
	}

	public String getCorrelationId() {
		return correlationId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
