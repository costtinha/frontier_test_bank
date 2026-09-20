package com.frontier.bank.balance.command;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.frontier.bank.balance.Balance;
import com.frontier.bank.balance.BalanceRepository;
import com.frontier.bank.balance.event.MoneyWithdrawn;
import com.frontier.bank.common.command.CommandHandler;
import com.frontier.bank.common.event.DomainEvent;
import com.frontier.bank.common.event.EventPublisher;
import com.frontier.bank.user.UserRepository;

/**
 * Handler de escrita: saque. Executa em transação própria, com lock pessimista
 * no saldo; o saldo negativo é rejeitado pela invariante de domínio em
 * {@link Balance#withdraw} (HTTP 422).
 * <p>
 * Publica {@link MoneyWithdrawn} na outbox dentro da mesma transação — um saque
 * rejeitado nunca gera evento de dinheiro movimentado.
 */
@Component
@Transactional
public class WithdrawHandler implements CommandHandler<WithdrawCommand, UUID> {

	private final BalanceRepository balanceRepository;
	private final EventPublisher eventPublisher;
	private final BalanceWriteGuard guard;

	public WithdrawHandler(BalanceRepository balanceRepository, UserRepository userRepository,
			EventPublisher eventPublisher) {
		this.balanceRepository = balanceRepository;
		this.eventPublisher = eventPublisher;
		this.guard = new BalanceWriteGuard(balanceRepository, userRepository);
	}

	@Override
	public UUID handle(WithdrawCommand command) {
		guard.ensureUserExists(command.userId());
		BigDecimal value = guard.normalize(command.amount());
		Balance balance = guard.lockOrCreate(command.userId());
		balance.withdraw(value);
		Balance saved = balanceRepository.saveAndFlush(balance);

		eventPublisher.publish(DomainEvent.of(new MoneyWithdrawn(
				UUID.randomUUID(), // transactionId — será persistido no ledger na Fase 3
				saved.getId(),
				command.userId(),
				value,
				saved.getAmount())));

		return saved.getId();
	}

}
