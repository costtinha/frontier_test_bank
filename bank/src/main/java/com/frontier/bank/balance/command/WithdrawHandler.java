package com.frontier.bank.balance.command;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.frontier.bank.balance.Balance;
import com.frontier.bank.balance.BalanceRepository;
import com.frontier.bank.balance.BalanceWriteGuard;
import com.frontier.bank.balance.event.MoneyWithdrawn;
import com.frontier.bank.common.command.CommandHandler;
import com.frontier.bank.common.event.DomainEvent;
import com.frontier.bank.common.event.EventPublisher;
import com.frontier.bank.ledger.LedgerEntry;
import com.frontier.bank.ledger.LedgerRepository;
import com.frontier.bank.user.UserRepository;

/**
 * Handler de escrita: saque. Executa em transação própria, com lock pessimista
 * no saldo; o saldo negativo é rejeitado pela invariante de domínio em
 * {@link Balance#withdraw} (HTTP 422).
 * <p>
 * Grava a entrada no <b>razão</b> e publica {@link MoneyWithdrawn} na mesma
 * transação — um saque rejeitado não gera nem entrada no extrato nem evento.
 */
@Component
@Transactional
public class WithdrawHandler implements CommandHandler<WithdrawCommand, UUID> {

	private final BalanceRepository balanceRepository;
	private final LedgerRepository ledgerRepository;
	private final EventPublisher eventPublisher;
	private final BalanceWriteGuard guard;

	public WithdrawHandler(BalanceRepository balanceRepository, LedgerRepository ledgerRepository,
			UserRepository userRepository, EventPublisher eventPublisher) {
		this.balanceRepository = balanceRepository;
		this.ledgerRepository = ledgerRepository;
		this.eventPublisher = eventPublisher;
		this.guard = new BalanceWriteGuard(balanceRepository, userRepository);
	}

	@Override
	public UUID handle(WithdrawCommand command) {
		guard.ensureUserExists(command.userId());
		BigDecimal value = guard.normalize(command.amount());
		Balance balance = guard.lockOrCreate(command.userId());

		BigDecimal balanceBefore = balance.getAmount();
		balance.withdraw(value);
		Balance saved = balanceRepository.saveAndFlush(balance);

		UUID transactionId = UUID.randomUUID();
		ledgerRepository.save(LedgerEntry.withdrawal(transactionId, saved.getId(), command.userId(),
				value, balanceBefore, saved.getAmount()));

		eventPublisher.publish(DomainEvent.of(new MoneyWithdrawn(
				transactionId,
				saved.getId(),
				command.userId(),
				value,
				saved.getAmount())));

		return saved.getId();
	}

}
