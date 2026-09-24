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
import com.frontier.bank.transfer.event.TransferDebitRefunded;
import com.frontier.bank.user.UserRepository;

/**
 * Compensação do passo de débito: devolve o valor à conta de origem.
 * <p>
 * Em banco, desfazer dinheiro é <b>sempre um lançamento novo</b> no razão — por
 * isso existe {@code TRANSFER_REFUND} e um evento próprio, e não uma remoção da
 * entrada anterior. A trilha contábil fica completa: débito, estorno e (se for o
 * caso) a rejeição.
 */
@Component
@Transactional
public class RefundTransferDebitHandler implements CommandHandler<RefundTransferDebitCommand, UUID> {

	private final BalanceRepository balanceRepository;
	private final LedgerRepository ledgerRepository;
	private final EventPublisher eventPublisher;
	private final BalanceWriteGuard guard;

	public RefundTransferDebitHandler(BalanceRepository balanceRepository, LedgerRepository ledgerRepository,
			UserRepository userRepository, EventPublisher eventPublisher) {
		this.balanceRepository = balanceRepository;
		this.ledgerRepository = ledgerRepository;
		this.eventPublisher = eventPublisher;
		this.guard = new BalanceWriteGuard(balanceRepository, userRepository);
	}

	@Override
	public UUID handle(RefundTransferDebitCommand command) {
		guard.ensureUserExists(command.sourceUserId());
		BigDecimal value = guard.normalize(command.amount());
		Balance balance = guard.lockOrCreate(command.sourceUserId());

		BigDecimal balanceBefore = balance.getAmount();
		balance.deposit(value);
		Balance saved = balanceRepository.saveAndFlush(balance);

		ledgerRepository.save(LedgerEntry.transferRefund(command.transferId(), saved.getId(),
				command.sourceUserId(), value, balanceBefore, saved.getAmount()));

		eventPublisher.publish(DomainEvent.of(new TransferDebitRefunded(command.transferId(), saved.getId(),
				command.sourceUserId(), value, saved.getAmount())));

		return saved.getId();
	}

}
