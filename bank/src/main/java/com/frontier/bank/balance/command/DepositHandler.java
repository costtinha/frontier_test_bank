package com.frontier.bank.balance.command;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.frontier.bank.balance.Balance;
import com.frontier.bank.balance.BalanceRepository;
import com.frontier.bank.balance.event.MoneyDeposited;
import com.frontier.bank.common.command.CommandHandler;
import com.frontier.bank.common.event.DomainEvent;
import com.frontier.bank.common.event.EventPublisher;
import com.frontier.bank.user.UserRepository;

/**
 * Handler de escrita: depósito. Executa em transação própria, com lock
 * pessimista no saldo — atômico sob concorrência.
 * <p>
 * Publica {@link MoneyDeposited} na outbox dentro da mesma transação: o evento
 * e o novo saldo são commitados juntos ou não existem.
 */
@Component
@Transactional
public class DepositHandler implements CommandHandler<DepositCommand, UUID> {

	private final BalanceRepository balanceRepository;
	private final EventPublisher eventPublisher;
	private final BalanceWriteGuard guard;

	public DepositHandler(BalanceRepository balanceRepository, UserRepository userRepository,
			EventPublisher eventPublisher) {
		this.balanceRepository = balanceRepository;
		this.eventPublisher = eventPublisher;
		this.guard = new BalanceWriteGuard(balanceRepository, userRepository);
	}

	@Override
	public UUID handle(DepositCommand command) {
		guard.ensureUserExists(command.userId());
		BigDecimal value = guard.normalize(command.amount());
		Balance balance = guard.lockOrCreate(command.userId());
		balance.deposit(value);
		Balance saved = balanceRepository.saveAndFlush(balance);

		eventPublisher.publish(DomainEvent.of(new MoneyDeposited(
				UUID.randomUUID(), // transactionId — será persistido no ledger na Fase 3
				saved.getId(),
				command.userId(),
				value,
				saved.getAmount())));

		return saved.getId();
	}

}
