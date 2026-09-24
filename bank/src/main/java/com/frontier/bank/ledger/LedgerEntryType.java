package com.frontier.bank.ledger;

/**
 * Natureza da entrada no razão. A direção (crédito/débito) é derivada do tipo —
 * a tabela guarda apenas o tipo e o valor sempre positivo.
 */
public enum LedgerEntryType {

	DEPOSIT(true),
	WITHDRAWAL(false),
	TRANSFER_DEBIT(false),
	TRANSFER_CREDIT(true),
	/** Estorno do débito de uma transferência compensada pela saga. */
	TRANSFER_REFUND(true);

	private final boolean credit;

	LedgerEntryType(boolean credit) {
		this.credit = credit;
	}

	/** {@code true} quando a entrada soma saldo (entrada de dinheiro). */
	public boolean isCredit() {
		return credit;
	}

}
