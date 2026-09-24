package com.frontier.bank.saga;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.frontier.bank.common.context.CorrelationContext;

/**
 * Retoma sagas pendentes: aquelas cuja próxima tentativa venceu (falha
 * transitória) e as que ficaram órfãs (processo caiu depois de registrar a saga
 * e antes de conduzi-la). Como o estado é durável e os passos são idempotentes,
 * retomar é simplesmente chamar o orquestrador de novo.
 */
@Component
public class SagaSweeper {

	private static final Logger log = LoggerFactory.getLogger(SagaSweeper.class);

	private final SagaInstanceRepository repository;
	private final SagaOrchestrator orchestrator;
	private final int batchSize;

	public SagaSweeper(SagaInstanceRepository repository, SagaOrchestrator orchestrator,
			@Value("${bank.saga.batch-size:20}") int batchSize) {
		this.repository = repository;
		this.orchestrator = orchestrator;
		this.batchSize = batchSize;
	}

	@Scheduled(fixedDelayString = "${bank.saga.poll-interval:5000}")
	public void resumePendingSagas() {
		try {
			List<SagaInstance> pending = repository.findPending(Instant.now(), Pageable.ofSize(batchSize));
			for (SagaInstance saga : pending) {
				CorrelationContext.set(saga.getCorrelationId());
				try {
					orchestrator.advance(saga.getId());
				} catch (RuntimeException e) {
					// uma saga com problema não pode travar a varredura das demais
					log.warn("Falha ao retomar a saga {}: {}", saga.getId(), e.getMessage());
				} finally {
					CorrelationContext.clear();
				}
			}
		} catch (RuntimeException e) {
			log.error("Falha ao varrer sagas pendentes: {}", e.getMessage(), e);
		}
	}

}
