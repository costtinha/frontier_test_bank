package com.frontier.bank.balance.command;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.frontier.bank.balance.Balance;
import com.frontier.bank.balance.BalanceRepository;
import com.frontier.bank.common.command.CommandHandler;
import com.frontier.bank.user.UserRepository;

/**
 * Handler de escrita: depósito. Executa em transação própria, com lock
 * pessimista no saldo — atômico sob concorrência.
 */
@Component
@Transactional
public class DepositHandler implements CommandHandler<DepositCommand, UUID> {

	private final BalanceRepository balanceRepository;
	private final BalanceWriteGuard guard;

	public DepositHandler(BalanceRepository balanceRepository, UserRepository userRepository) {
		this.balanceRepository = balanceRepository;
		this.guard = new BalanceWriteGuard(balanceRepository, userRepository);
	}

	@Override
	public UUID handle(DepositCommand command) {
		guard.ensureUserExists(command.userId());
		BigDecimal value = guard.normalize(command.amount());
		Balance balance = guard.lockOrCreate(command.userId());
		balance.deposit(value);
		return balanceRepository.saveAndFlush(balance).getId();
	}

}
