package com.frontier.bank.transfer.command;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.frontier.bank.common.command.CommandHandler;
import com.frontier.bank.common.event.DomainEvent;
import com.frontier.bank.common.event.EventPublisher;
import com.frontier.bank.transfer.event.TransferFailed;

/**
 * Publica o desfecho negativo da saga ({@code TransferFailed}) depois que os
 * efeitos já foram desfeitos. Roda como passo sintético ({@code onFailed}) e
 * portanto entra no mesmo dedupe — nunca duplica o evento.
 */
@Component
@Transactional
public class FailTransferHandler implements CommandHandler<FailTransferCommand, UUID> {

	private final EventPublisher eventPublisher;

	public FailTransferHandler(EventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	@Override
	public UUID handle(FailTransferCommand command) {
		eventPublisher.publish(DomainEvent.of(new TransferFailed(command.transferId(), command.sourceUserId(),
				command.targetUserId(), command.amount(), reason(command))));
		return command.transferId();
	}

	private String reason(FailTransferCommand command) {
		return command.reason() == null ? "SAGA_REJEITADA" : command.reason();
	}

}
