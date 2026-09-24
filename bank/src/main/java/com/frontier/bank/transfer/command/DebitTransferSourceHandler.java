package com.frontier.bank.transfer.command;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.frontier.bank.balance.Balance;
import com.frontier.bank.balance.BalanceRepository;
import com.frontier.bank.balance.BalanceWriteGuard;
import com.frontier.bank.common.command.CommandHandler;
import com.frontier.bank.common.error.TransferRejectedException;
import com.frontier.bank.common.event.DomainEvent;
import com.frontier.bank.common.event.EventPublisher;
import com.frontier.bank.ledger.LedgerEntry;
import com.frontier.bank.ledger.LedgerRepository;
import com.frontier.bank.transfer.event.TransferDebited;
import com.frontier.bank.user.UserRepository;

/**
 * Passo 1 da saga: debita a conta de origem.
 * <p>
 * Saldo insuficiente é <b>rejeição de negócio</b> (não falha transitória): a saga
 * compensa — e, como este é o primeiro passo, não há efeito a desfazer.
 */
@Component
@Transactional
public class DebitTransferSourceHandler implements CommandHandler<DebitTransferSourceCommand, UUID> {

	private final BalanceRepository balanceRepository;
	private final LedgerRepository ledgerRepository;
	private final EventPublisher eventPublisher;
	private final BalanceWriteGuard guard;

	public DebitTransferSourceHandler(BalanceRepository balanceRepository, LedgerRepository ledgerRepository,
			UserRepository userRepository, EventPublisher eventPublisher) {
		this.balanceRepository = balanceRepository;
		this.ledgerRepository = ledgerRepository;
		this.eventPublisher = eventPublisher;
		this.guard = new BalanceWriteGuard(balanceRepository, userRepository);
	}

	@Override
	public UUID handle(DebitTransferSourceCommand command) {
		guard.ensureUserExists(command.sourceUserId());
		BigDecimal value = guard.normalize(command.amount());
		Balance balance = guard.lockOrCreate(command.sourceUserId());

		BigDecimal balanceBefore = balance.getAmount();
		if (balanceBefore.compareTo(value) < 0) {
			throw new TransferRejectedException(
					"Saldo insuficiente: disponível %s, solicitado %s".formatted(balanceBefore, value));
		}

		balance.withdraw(value);
		Balance saved = balanceRepository.saveAndFlush(balance);

		ledgerRepository.save(LedgerEntry.transferDebit(command.transferId(), saved.getId(),
				command.sourceUserId(), value, balanceBefore, saved.getAmount()));

		eventPublisher.publish(DomainEvent.of(new TransferDebited(command.transferId(), saved.getId(),
				command.sourceUserId(), value, saved.getAmount())));

		return saved.getId();
	}

}
