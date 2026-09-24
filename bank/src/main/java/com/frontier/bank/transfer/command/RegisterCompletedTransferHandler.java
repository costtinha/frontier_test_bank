package com.frontier.bank.transfer.command;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.frontier.bank.common.command.CommandHandler;
import com.frontier.bank.common.event.DomainEvent;
import com.frontier.bank.common.event.EventPublisher;
import com.frontier.bank.transfer.Transfer;
import com.frontier.bank.transfer.TransferRepository;
import com.frontier.bank.transfer.event.TransferCompleted;

/**
 * Passo final da saga: efetiva a transferência (registro durável + evento de
 * conclusão). Idempotente: o dedupe da saga já impede reexecução, e a verificação
 * pela chave de idempotência é a segunda barreira.
 */
@Component
@Transactional
public class RegisterCompletedTransferHandler
		implements CommandHandler<RegisterCompletedTransferCommand, UUID> {

	private final TransferRepository transferRepository;
	private final EventPublisher eventPublisher;

	public RegisterCompletedTransferHandler(TransferRepository transferRepository, EventPublisher eventPublisher) {
		this.transferRepository = transferRepository;
		this.eventPublisher = eventPublisher;
	}

	@Override
	public UUID handle(RegisterCompletedTransferCommand command) {
		if (transferRepository.findByIdempotencyKey(command.idempotencyKey()).isEmpty()) {
			transferRepository.save(Transfer.completed(command.transferId(), command.idempotencyKey(),
					command.sourceUserId(), command.targetUserId(), command.amount(), Instant.now()));
		}

		eventPublisher.publish(DomainEvent.of(new TransferCompleted(command.transferId(),
				command.sourceUserId(), command.targetUserId(), command.amount())));

		return command.transferId();
	}

}
