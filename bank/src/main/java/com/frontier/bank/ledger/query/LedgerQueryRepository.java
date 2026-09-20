package com.frontier.bank.ledger.query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.frontier.bank.ledger.LedgerEntry;

/**
 * Repositório de LEITURA do razão (extrato): apenas consultas, sem operações de
 * escrita. Lê o próprio ledger — no caso do extrato ele já é o read model ideal:
 * append-only, ordenado e com o saldo após cada movimentação.
 */
public interface LedgerQueryRepository extends Repository<LedgerEntry, UUID> {

	@Query("""
			SELECT new com.frontier.bank.ledger.query.StatementEntry(
				l.id, l.transactionId, l.type, l.amount, l.balanceAfter, l.occurredAt)
			FROM LedgerEntry l
			WHERE l.userId = :userId
			ORDER BY l.occurredAt ASC, l.id ASC
			""")
	List<StatementEntry> findFirstPage(@Param("userId") UUID userId, Pageable pageable);

	@Query("""
			SELECT new com.frontier.bank.ledger.query.StatementEntry(
				l.id, l.transactionId, l.type, l.amount, l.balanceAfter, l.occurredAt)
			FROM LedgerEntry l
			WHERE l.userId = :userId
			  AND (l.occurredAt > :occurredAt OR (l.occurredAt = :occurredAt AND l.id > :id))
			ORDER BY l.occurredAt ASC, l.id ASC
			""")
	List<StatementEntry> findAfter(@Param("userId") UUID userId, @Param("occurredAt") Instant occurredAt,
			@Param("id") UUID id, Pageable pageable);

	@Query("""
			SELECT new com.frontier.bank.ledger.query.StatementEntry(
				l.id, l.transactionId, l.type, l.amount, l.balanceAfter, l.occurredAt)
			FROM LedgerEntry l
			WHERE l.userId = :userId
			  AND (l.occurredAt < :occurredAt OR (l.occurredAt = :occurredAt AND l.id < :id))
			ORDER BY l.occurredAt DESC, l.id DESC
			""")
	List<StatementEntry> findBefore(@Param("userId") UUID userId, @Param("occurredAt") Instant occurredAt,
			@Param("id") UUID id, Pageable pageable);

	@Query("""
			SELECT new com.frontier.bank.ledger.query.StatementEntry(
				l.id, l.transactionId, l.type, l.amount, l.balanceAfter, l.occurredAt)
			FROM LedgerEntry l
			WHERE l.userId = :userId
			ORDER BY l.occurredAt DESC, l.id DESC
			""")
	List<StatementEntry> findLastPage(@Param("userId") UUID userId, Pageable pageable);

}
