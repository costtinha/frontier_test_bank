package com.frontier.bank.ledger;

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
 * Entrada do livro-razão: registro <b>append-only</b> de uma movimentação de
 * saldo. Não há setters nem atualização — uma vez gravada, a entrada é imutável.
 * <p>
 * Cada entrada guarda o saldo antes e depois, o que permite auditar e
 * reconciliar o saldo da conta com o histórico. Transferências geram duas
 * entradas (débito e crédito) com o mesmo {@code transactionId}.
 */
@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {

	@Id
	private UUID id;

	/** Agrupa as entradas de uma mesma operação financeira. */
	@Column(name = "transaction_id", nullable = false)
	private UUID transactionId;

	@Column(name = "balance_id", nullable = false)
	private UUID balanceId;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Enumerated(EnumType.STRING)
	@Column(name = "entry_type", nullable = false, length = 30)
	private LedgerEntryType type;

	/** Sempre positivo: a direção vem do {@link LedgerEntryType}. */
	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal amount;

	@Column(name = "balance_before", nullable = false, precision = 19, scale = 2)
	private BigDecimal balanceBefore;

	@Column(name = "balance_after", nullable = false, precision = 19, scale = 2)
	private BigDecimal balanceAfter;

	@Column(name = "occurred_at", nullable = false)
	private Instant occurredAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected LedgerEntry() {
		// construtor exigido pelo JPA
	}

	public static LedgerEntry deposit(UUID transactionId, UUID balanceId, UUID userId, BigDecimal amount,
			BigDecimal balanceBefore, BigDecimal balanceAfter) {
		return create(transactionId, balanceId, userId, LedgerEntryType.DEPOSIT, amount, balanceBefore, balanceAfter);
	}

	public static LedgerEntry withdrawal(UUID transactionId, UUID balanceId, UUID userId, BigDecimal amount,
			BigDecimal balanceBefore, BigDecimal balanceAfter) {
		return create(transactionId, balanceId, userId, LedgerEntryType.WITHDRAWAL, amount, balanceBefore, balanceAfter);
	}

	public static LedgerEntry transferDebit(UUID transactionId, UUID balanceId, UUID userId, BigDecimal amount,
			BigDecimal balanceBefore, BigDecimal balanceAfter) {
		return create(transactionId, balanceId, userId, LedgerEntryType.TRANSFER_DEBIT, amount, balanceBefore, balanceAfter);
	}

	public static LedgerEntry transferCredit(UUID transactionId, UUID balanceId, UUID userId, BigDecimal amount,
			BigDecimal balanceBefore, BigDecimal balanceAfter) {
		return create(transactionId, balanceId, userId, LedgerEntryType.TRANSFER_CREDIT, amount, balanceBefore, balanceAfter);
	}

	private static LedgerEntry create(UUID transactionId, UUID balanceId, UUID userId, LedgerEntryType type,
			BigDecimal amount, BigDecimal balanceBefore, BigDecimal balanceAfter) {
		LedgerEntry entry = new LedgerEntry();
		entry.id = UUID.randomUUID();
		entry.transactionId = transactionId;
		entry.balanceId = balanceId;
		entry.userId = userId;
		entry.type = type;
		entry.amount = amount;
		entry.balanceBefore = balanceBefore;
		entry.balanceAfter = balanceAfter;
		entry.occurredAt = Instant.now();
		entry.createdAt = entry.occurredAt;
		return entry;
	}

	public UUID getId() {
		return id;
	}

	public UUID getTransactionId() {
		return transactionId;
	}

	public UUID getBalanceId() {
		return balanceId;
	}

	public UUID getUserId() {
		return userId;
	}

	public LedgerEntryType getType() {
		return type;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public BigDecimal getBalanceBefore() {
		return balanceBefore;
	}

	public BigDecimal getBalanceAfter() {
		return balanceAfter;
	}

	public Instant getOccurredAt() {
		return occurredAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
