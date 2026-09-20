package com.frontier.bank.balance.command;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.frontier.bank.balance.Balance;
import com.frontier.bank.balance.BalanceRepository;
import com.frontier.bank.balance.BalanceWriteGuard;
import com.frontier.bank.balance.event.MoneyDeposited;
import com.frontier.bank.common.command.CommandHandler;
import com.frontier.bank.common.event.DomainEvent;
import com.frontier.bank.common.event.EventPublisher;
import com.frontier.bank.ledger.LedgerEntry;
import com.frontier.bank.ledger.LedgerRepository;
import com.frontier.bank.user.UserRepository;

/**
 * Handler de escrita: depósito. Executa em transação própria, com lock
 * pessimista no saldo — atômico sob concorrência.
 * <p>
 * Grava a entrada no <b>razão</b> e publica {@link MoneyDeposited} na mesma
 * transação da mudança de saldo: saldo, extrato e evento commitam juntos ou não
 * existem. O {@code transactionId} é o mesmo no razão e no evento, permitindo
 * correlacionar as duas visões.
 */
@Component
@Transactional
public class DepositHandler implements CommandHandler<DepositCommand, UUID> {

	private final BalanceRepository balanceRepository;
	private final LedgerRepository ledgerRepository;
	private final EventPublisher eventPublisher;
	private final BalanceWriteGuard guard;

	public DepositHandler(BalanceRepository balanceRepository, LedgerRepository ledgerRepository,
			UserRepository userRepository, EventPublisher eventPublisher) {
		this.balanceRepository = balanceRepository;
		this.ledgerRepository = ledgerRepository;
		this.eventPublisher = eventPublisher;
		this.guard = new BalanceWriteGuard(balanceRepository, userRepository);
	}

	@Override
	public UUID handle(DepositCommand command) {
		guard.ensureUserExists(command.userId());
		BigDecimal value = guard.normalize(command.amount());
		Balance balance = guard.lockOrCreate(command.userId());

		BigDecimal balanceBefore = balance.getAmount();
		balance.deposit(value);
		Balance saved = balanceRepository.saveAndFlush(balance);

		UUID transactionId = UUID.randomUUID();
		ledgerRepository.save(LedgerEntry.deposit(transactionId, saved.getId(), command.userId(),
				value, balanceBefore, saved.getAmount()));

		eventPublisher.publish(DomainEvent.of(new MoneyDeposited(
				transactionId,
				saved.getId(),
				command.userId(),
				value,
				saved.getAmount())));

		return saved.getId();
	}

}
