package com.frontier.bank.audit;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Linha única usada para serializar a escrita da cadeia de auditoria.
 * <p>
 * O recorder trava esta linha ({@code FOR UPDATE}) antes de ler o último hash:
 * transações concorrentes escrevem em fila e a cadeia permanece linear, sem
 * ramificações.
 */
@Entity
@Table(name = "audit_chain_guard")
public class AuditChainGuard {

	public static final short SINGLE_ROW_ID = 1;

	@Id
	private Short id;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected AuditChainGuard() {
		// construtor exigido pelo JPA
	}

	public Short getId() {
		return id;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
