package com.frontier.bank.balance;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.frontier.bank.common.error.InsufficientFundsException;
import com.frontier.bank.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Recursos financeiros do cliente (relação 1:1 com {@link User}).
 * <p>
 * Valores monetários usam {@link BigDecimal} (nunca ponto flutuante) com
 * precisão NUMERIC(19,2). {@code @Version} + lock pessimista no repositório
 * protegem depósitos/saques concorrentes.
 */
@Entity
@Table(name = "balances")
public class Balance {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal amount = BigDecimal.ZERO.setScale(2);

	@Version
	@Column(nullable = false)
	private long version;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Balance() {
		// construtor exigido pelo JPA
	}

	public Balance(User user) {
		this.user = user;
	}

	/**
	 * Credita um valor (normalizado para 2 casas decimais).
	 */
	public void deposit(BigDecimal value) {
		this.amount = this.amount.add(value);
	}

	/**
	 * Debita um valor, rejeitando saldo negativo (invariante de domínio).
	 *
	 * @throws InsufficientFundsException se o valor exceder o saldo disponível
	 */
	public void withdraw(BigDecimal value) {
		if (this.amount.compareTo(value) < 0) {
			throw new InsufficientFundsException(this.amount, value);
		}
		this.amount = this.amount.subtract(value);
	}

	public UUID getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public long getVersion() {
		return version;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
