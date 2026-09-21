package com.frontier.bank.transfer.command;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.frontier.bank.balance.Balance;
import com.frontier.bank.balance.BalanceRepository;
import com.frontier.bank.balance.BalanceWriteGuard;
import com.frontier.bank.common.command.CommandHandler;
import com.frontier.bank.common.error.IdempotencyConflictException;
import com.frontier.bank.common.error.ResourceNotFoundException;
import com.frontier.bank.common.event.DomainEvent;
import com.frontier.bank.common.event.EventPublisher;
import com.frontier.bank.common.observability.BankMetrics;
import com.frontier.bank.ledger.LedgerEntry;
import com.frontier.bank.ledger.LedgerRepository;
import com.frontier.bank.transfer.Transfer;
import com.frontier.bank.transfer.TransferRepository;
import com.frontier.bank.transfer.event.TransferCompleted;
import com.frontier.bank.transfer.event.TransferCredited;
import com.frontier.bank.transfer.event.TransferDebited;
import com.frontier.bank.transfer.event.TransferFailed;
import com.frontier.bank.transfer.event.TransferInitiated;
import com.frontier.bank.user.UserRepository;

/**
 * Handler de escrita: transferência entre contas.
 * <p>
 * <b>Consistência:</b> as duas contas ficam no mesmo banco, então a operação é
 * uma única transação ACID — travar dois agregados e commitar junto é o que dá
 * a garantia mais forte para dinheiro. O fluxo já está estruturado como os
 * passos de uma saga ({@code TransferInitiated → TransferDebited →
 * TransferCredited → TransferCompleted/Failed}, com razão e eventos) para que,
 * quando as contas forem separadas em serviços, os mesmos passos virem etapas
 * distribuídas com compensação — sem inventar um saga onde o ACID basta.
 * <p>
 * <b>Concorrência:</b> as contas são travadas sempre na mesma ordem (por id da
 * conta), o que evita deadlock em transferências cruzadas simultâneas.
 * <p>
 * <b>Idempotência:</b> a {@code Idempotency-Key} é única no banco; um retry do
 * cliente devolve o desfecho já registrado sem movimentar dinheiro de novo.
 */
@Component
@Transactional
public class TransferMoneyHandler implements CommandHandler<TransferMoneyCommand, TransferResult> {

	private final BalanceRepository balanceRepository;
	private final TransferRepository transferRepository;
	private final LedgerRepository ledgerRepository;
	private final EventPublisher eventPublisher;
	private final BankMetrics metrics;
	private final BalanceWriteGuard guard;

	public TransferMoneyHandler(BalanceRepository balanceRepository, TransferRepository transferRepository,
			LedgerRepository ledgerRepository, UserRepository userRepository, EventPublisher eventPublisher,
			BankMetrics metrics) {
		this.balanceRepository = balanceRepository;
		this.transferRepository = transferRepository;
		this.ledgerRepository = ledgerRepository;
		this.eventPublisher = eventPublisher;
		this.metrics = metrics;
		this.guard = new BalanceWriteGuard(balanceRepository, userRepository);
	}

	@Override
	public TransferResult handle(TransferMoneyCommand command) {
		guard.ensureUserExists(command.sourceUserId());
		guard.ensureUserExists(command.targetUserId());
		BigDecimal value = guard.normalize(command.amount());

		if (command.sourceUserId().equals(command.targetUserId())) {
			throw new IllegalArgumentException("Conta de origem e destino devem ser diferentes");
		}

		TransferResult replay = replayOf(command.idempotencyKey(), command.sourceUserId(), command.targetUserId(), value);
		if (replay != null) {
			return replay;
		}

		UUID transferId = UUID.randomUUID();
		Instant occurredAt = Instant.now();
		eventPublisher.publish(DomainEvent.of(new TransferInitiated(
				transferId, command.sourceUserId(), command.targetUserId(), value)));

		Map<UUID, Balance> locked = lockAccountsInOrder(command.sourceUserId(), command.targetUserId());
		Balance sourceBalance = locked.get(command.sourceUserId());
		Balance targetBalance = locked.get(command.targetUserId());

		BigDecimal sourceBefore = sourceBalance.getAmount();
		if (sourceBefore.compareTo(value) < 0) {
			// rejeição de negócio: nada muda de estado, mas o fato é registrado
			eventPublisher.publish(DomainEvent.of(new TransferFailed(
					transferId, command.sourceUserId(), command.targetUserId(), value, "SALDO_INSUFICIENTE")));
			metrics.transferRejected();
			return TransferResult.failed(transferId, command.sourceUserId(), command.targetUserId(), value,
					occurredAt, "Saldo insuficiente: disponível %s, solicitado %s".formatted(sourceBefore, value));
		}

		BigDecimal targetBefore = targetBalance.getAmount();
		sourceBalance.withdraw(value);
		targetBalance.deposit(value);
		Balance savedSource = balanceRepository.saveAndFlush(sourceBalance);
		Balance savedTarget = balanceRepository.saveAndFlush(targetBalance);

		persistTransfer(transferId, command, value, occurredAt);

		// razão: as duas pontas da mesma operação compartilham o transactionId
		ledgerRepository.save(LedgerEntry.transferDebit(transferId, savedSource.getId(),
				command.sourceUserId(), value, sourceBefore, savedSource.getAmount()));
		ledgerRepository.save(LedgerEntry.transferCredit(transferId, savedTarget.getId(),
				command.targetUserId(), value, targetBefore, savedTarget.getAmount()));

		eventPublisher.publish(DomainEvent.of(new TransferDebited(transferId, savedSource.getId(),
				command.sourceUserId(), value, savedSource.getAmount())));
		eventPublisher.publish(DomainEvent.of(new TransferCredited(transferId, savedTarget.getId(),
				command.targetUserId(), value, savedTarget.getAmount())));
		eventPublisher.publish(DomainEvent.of(new TransferCompleted(
				transferId, command.sourceUserId(), command.targetUserId(), value)));

		metrics.transferCompleted();
		return TransferResult.completed(transferId, command.sourceUserId(), command.targetUserId(), value, occurredAt);
	}

	/**
	 * @return o desfecho já registrado quando a chave se repete; {@code null} quando é uma nova transferência
	 * @throws IdempotencyConflictException se a chave for reutilizada com outros parâmetros
	 */
	private TransferResult replayOf(String idempotencyKey, UUID sourceUserId, UUID targetUserId, BigDecimal value) {
		Optional<Transfer> existing = transferRepository.findByIdempotencyKey(idempotencyKey);
		if (existing.isEmpty()) {
			return null;
		}
		Transfer transfer = existing.get();
		if (!transfer.matches(sourceUserId, targetUserId, value)) {
			throw new IdempotencyConflictException(idempotencyKey);
		}
		return TransferResult.replayed(transfer);
	}

	private void persistTransfer(UUID transferId, TransferMoneyCommand command, BigDecimal value, Instant occurredAt) {
		try {
			transferRepository.saveAndFlush(Transfer.completed(transferId, command.idempotencyKey(),
					command.sourceUserId(), command.targetUserId(), value, occurredAt));
		} catch (DataIntegrityViolationException e) {
			// requisição concorrente com a mesma chave: a transação inteira é
			// revertida (nenhum dinheiro se move) e o cliente pode repetir
			throw new IdempotencyConflictException(command.idempotencyKey());
		}
	}

	private Map<UUID, Balance> lockAccountsInOrder(UUID sourceUserId, UUID targetUserId) {
		Map<UUID, UUID> balanceIdByUser = balanceRepository
				.findAllByUserIdIn(List.of(sourceUserId, targetUserId)).stream()
				.collect(Collectors.toMap(balance -> balance.getUser().getId(), Balance::getId));

		for (UUID userId : List.of(sourceUserId, targetUserId)) {
			if (!balanceIdByUser.containsKey(userId)) {
				throw new ResourceNotFoundException("saldo", userId);
			}
		}

		List<UUID> lockOrder = List.of(sourceUserId, targetUserId).stream()
				.sorted(Comparator.comparing(balanceIdByUser::get))
				.toList();

		Map<UUID, Balance> locked = new HashMap<>();
		for (UUID userId : lockOrder) {
			locked.put(userId, balanceRepository.findByUserIdForUpdate(userId)
					.orElseThrow(() -> new ResourceNotFoundException("saldo", userId)));
		}
		return locked;
	}

}
