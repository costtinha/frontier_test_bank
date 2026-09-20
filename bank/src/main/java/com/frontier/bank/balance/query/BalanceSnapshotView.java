package com.frontier.bank.balance.query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Read model do saldo (tabela {@code balance_snapshot}).
 * <p>
 * Pertence ao lado de leitura: é escrito <b>apenas</b> pelo projector (a partir
 * dos eventos) e lido pelo {@link BalanceQueryRepository}. O {@code amount} é
 * sempre o saldo resultante do último evento aplicado — nunca um acumulado
 * incremental — o que torna a projeção idempotente.
 */
@Entity
@Table(name = "balance_snapshot")
public class BalanceSnapshotView {

	@Id
	@Column(name = "balance_id")
	private UUID balanceId;

	@Column(name = "user_id", nullable = false, unique = true)
	private UUID userId;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal amount;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "last_event_id", nullable = false)
	private UUID lastEventId;

	protected BalanceSnapshotView() {
		// construtor exigido pelo JPA
	}

	public static BalanceSnapshotView open(UUID balanceId, UUID userId, BigDecimal amount,
			Instant occurredAt, UUID eventId) {
		BalanceSnapshotView view = new BalanceSnapshotView();
		view.balanceId = balanceId;
		view.userId = userId;
		view.amount = amount;
		view.createdAt = occurredAt;
		view.updatedAt = occurredAt;
		view.lastEventId = eventId;
		return view;
	}

	public void apply(BigDecimal newAmount, Instant occurredAt, UUID eventId) {
		this.amount = newAmount;
		this.updatedAt = occurredAt;
		this.lastEventId = eventId;
	}

	public UUID getBalanceId() {
		return balanceId;
	}

	public UUID getUserId() {
		return userId;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public UUID getLastEventId() {
		return lastEventId;
	}

}
