package com.frontier.bank.transfer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Transferência entre duas contas do banco.
 * <p>
 * A {@code idempotencyKey} (cabeçalho {@code Idempotency-Key}) tem restrição de
 * unicidade no banco: um retry do cliente reencontra o registro e recebe o mesmo
 * desfecho, sem movimentar dinheiro duas vezes.
 * <p>
 * Apenas transferências concluídas são persistidas — uma tentativa rejeitada
 * (saldo insuficiente) não deixa registro aqui, então o cliente pode tentar de
 * novo com a mesma chave depois de ajustar o saldo.
 */
@Entity
@Table(name = "transfers")
public class Transfer {

	@Id
	private UUID id;

	@Column(name = "idempotency_key", nullable = false, unique = true, length = 80)
	private String idempotencyKey;

	@Column(name = "source_user_id", nullable = false)
	private UUID sourceUserId;

	@Column(name = "target_user_id", nullable = false)
	private UUID targetUserId;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal amount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TransferStatus status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected Transfer() {
		// construtor exigido pelo JPA
	}

	public static Transfer completed(UUID id, String idempotencyKey, UUID sourceUserId, UUID targetUserId,
			BigDecimal amount, Instant createdAt) {
		Transfer transfer = new Transfer();
		transfer.id = id;
		transfer.idempotencyKey = idempotencyKey;
		transfer.sourceUserId = sourceUserId;
		transfer.targetUserId = targetUserId;
		transfer.amount = amount;
		transfer.status = TransferStatus.COMPLETED;
		transfer.createdAt = createdAt;
		return transfer;
	}

	/** Confere se a chave de idempotência está sendo reutilizada com os mesmos parâmetros. */
	public boolean matches(UUID sourceUserId, UUID targetUserId, BigDecimal amount) {
		return this.sourceUserId.equals(sourceUserId)
				&& this.targetUserId.equals(targetUserId)
				&& this.amount.compareTo(amount) == 0;
	}

	public UUID getId() {
		return id;
	}

	public String getIdempotencyKey() {
		return idempotencyKey;
	}

	public UUID getSourceUserId() {
		return sourceUserId;
	}

	public UUID getTargetUserId() {
		return targetUserId;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public TransferStatus getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
