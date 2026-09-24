package com.frontier.bank.transfer.command;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.frontier.bank.balance.Balance;
import com.frontier.bank.balance.BalanceRepository;
import com.frontier.bank.balance.BalanceWriteGuard;
import com.frontier.bank.common.command.CommandHandler;
import com.frontier.bank.common.event.DomainEvent;
import com.frontier.bank.common.event.EventPublisher;
import com.frontier.bank.ledger.LedgerEntry;
import com.frontier.bank.ledger.LedgerRepository;
import com.frontier.bank.transfer.event.TransferCredited;
import com.frontier.bank.user.UserRepository;

/**
 * Passo 2 da saga: credita a conta de destino.
 * <p>
 * Se este passo falhar, a compensação do passo anterior (estorno do débito)
 * devolve o dinheiro à origem.
 */
@Component
@Transactional
public class CreditTransferTargetHandler implements CommandHandler<CreditTransferTargetCommand, UUID> {

	private final BalanceRepository balanceRepository;
	private final LedgerRepository ledgerRepository;
	private final EventPublisher eventPublisher;
	private final BalanceWriteGuard guard;

	public CreditTransferTargetHandler(BalanceRepository balanceRepository, LedgerRepository ledgerRepository,
			UserRepository userRepository, EventPublisher eventPublisher) {
		this.balanceRepository = balanceRepository;
		this.ledgerRepository = ledgerRepository;
		this.eventPublisher = eventPublisher;
		this.guard = new BalanceWriteGuard(balanceRepository, userRepository);
	}

	@Override
	public UUID handle(CreditTransferTargetCommand command) {
		guard.ensureUserExists(command.targetUserId());
		BigDecimal value = guard.normalize(command.amount());
		Balance balance = guard.lockOrCreate(command.targetUserId());

		BigDecimal balanceBefore = balance.getAmount();
		balance.deposit(value);
		Balance saved = balanceRepository.saveAndFlush(balance);

		ledgerRepository.save(LedgerEntry.transferCredit(command.transferId(), saved.getId(),
				command.targetUserId(), value, balanceBefore, saved.getAmount()));

		eventPublisher.publish(DomainEvent.of(new TransferCredited(command.transferId(), saved.getId(),
				command.targetUserId(), value, saved.getAmount())));

		return saved.getId();
	}

}
