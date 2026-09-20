package com.frontier.bank.ledger.query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.frontier.bank.ledger.LedgerEntryType;

/**
 * Projeção de leitura de uma linha do extrato. O tipo da entrada indica a
 * direção (DEPOSIT/TRANSFER_CREDIT somam; WITHDRAWAL/TRANSFER_DEBIT subtraem) e
 * {@code balanceAfter} dá o saldo da conta logo após a movimentação.
 */
public record StatementEntry(
		UUID id,
		UUID transactionId,
		LedgerEntryType type,
		BigDecimal amount,
		BigDecimal balanceAfter,
		Instant occurredAt) {
}
