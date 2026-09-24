package com.frontier.bank.transfer.saga;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.frontier.bank.common.context.CorrelationContext;
import com.frontier.bank.common.error.IdempotencyConflictException;
import com.frontier.bank.common.event.DomainEvent;
import com.frontier.bank.common.event.EventPublisher;
import com.frontier.bank.saga.SagaInstance;
import com.frontier.bank.saga.SagaInstanceRepository;
import com.frontier.bank.transfer.command.TransferMoneyCommand;
import com.frontier.bank.transfer.event.TransferInitiated;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Inicia (ou recupera) a saga de transferência em transação própria.
 * <p>
 * A saga é registrada <b>antes</b> de qualquer movimento de dinheiro e já nasce
 * elegível para o sweeper: se o processo cair logo depois, a transferência é
 * retomada em vez de ficar pela metade.
 * <p>
 * A chave de idempotência é única na tabela de sagas — repetir a requisição
 * reencontra a mesma saga (mesmo desfecho) em vez de transferir de novo.
 */
@Component
public class TransferSagaStarter {

	private static final Logger log = LoggerFactory.getLogger(TransferSagaStarter.class);

	private final SagaInstanceRepository sagaRepository;
	private final ObjectMapper objectMapper;
	private final EventPublisher eventPublisher;

	public TransferSagaStarter(SagaInstanceRepository sagaRepository, ObjectMapper objectMapper,
			EventPublisher eventPublisher) {
		this.sagaRepository = sagaRepository;
		this.objectMapper = objectMapper;
		this.eventPublisher = eventPublisher;
	}

	public record Started(UUID sagaId, TransferSagaContext context) {
	}

	@Transactional
	public Started start(TransferMoneyCommand command) {
		Optional<SagaInstance> existing = sagaRepository.findByIdempotencyKey(command.idempotencyKey());
		if (existing.isPresent()) {
			SagaInstance saga = existing.get();
			TransferSagaContext context = contextOf(saga);
			if (!context.matches(command.sourceUserId(), command.targetUserId(), command.amount())) {
				throw new IdempotencyConflictException(command.idempotencyKey());
			}
			log.debug("Saga {} reutilizada para a Idempotency-Key {}", saga.getId(), command.idempotencyKey());
			return new Started(saga.getId(), context);
		}

		TransferSagaContext context = TransferSagaContextFactory.of(command);
		SagaInstance saga = sagaRepository.save(SagaInstance.start(
				TransferSagaDefinition.SAGA_TYPE,
				writeContext(context),
				command.idempotencyKey(),
				CorrelationContext.getOrCreate(),
				Instant.now()));

		// fato: a transferência foi aceita para processamento pela saga
		eventPublisher.publish(DomainEvent.of(new TransferInitiated(context.transferId(),
				context.sourceUserId(), context.targetUserId(), context.amount())));

		log.info("Saga {} iniciada para a transferência {}", saga.getId(), context.transferId());
		return new Started(saga.getId(), context);
	}

	TransferSagaContext contextOf(SagaInstance saga) {
		try {
			return objectMapper.readValue(saga.getPayload(), TransferSagaContext.class);
		} catch (JacksonException e) {
			throw new IllegalStateException("Contexto inválido da saga " + saga.getId(), e);
		}
	}

	private String writeContext(TransferSagaContext context) {
		try {
			return objectMapper.writeValueAsString(context);
		} catch (JacksonException e) {
			throw new IllegalStateException("Falha ao serializar o contexto da transferência", e);
		}
	}

}
