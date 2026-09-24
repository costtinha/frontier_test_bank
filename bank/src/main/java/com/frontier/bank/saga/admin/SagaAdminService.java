package com.frontier.bank.saga.admin;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.frontier.bank.common.error.ResourceNotFoundException;
import com.frontier.bank.saga.SagaInstance;
import com.frontier.bank.saga.SagaInstanceRepository;
import com.frontier.bank.saga.SagaOrchestrator;
import com.frontier.bank.saga.SagaRegistry;
import com.frontier.bank.saga.SagaState;

/**
 * Operação de sagas: inspeção e retomada manual (antecipando o sweeper).
 */
@Service
public class SagaAdminService {

	private static final int MAX_PAGE_SIZE = 200;

	private final SagaInstanceRepository repository;
	private final SagaRegistry registry;
	private final SagaOrchestrator orchestrator;

	public SagaAdminService(SagaInstanceRepository repository, SagaRegistry registry,
			SagaOrchestrator orchestrator) {
		this.repository = repository;
		this.registry = registry;
		this.orchestrator = orchestrator;
	}

	@Transactional(readOnly = true)
	public List<SagaStatus> list(SagaState state, int limit) {
		int pageSize = Math.max(1, Math.min(limit, MAX_PAGE_SIZE));
		List<SagaInstance> sagas = state == null
				? repository.findAllByOrderByCreatedAtDesc(PageRequest.ofSize(pageSize))
				: repository.findAllByStateOrderByCreatedAtAsc(state, PageRequest.ofSize(pageSize));
		return sagas.stream().map(this::toStatus).toList();
	}

	/** Retoma a saga agora, sem esperar o backoff agendado. */
	public SagaStatus retry(UUID sagaId) {
		orchestrator.advance(sagaId);
		return toStatus(load(sagaId));
	}

	private SagaInstance load(UUID sagaId) {
		return repository.findById(sagaId)
				.orElseThrow(() -> new ResourceNotFoundException("saga", sagaId));
	}

	private SagaStatus toStatus(SagaInstance saga) {
		return new SagaStatus(
				saga.getId(),
				saga.getSagaType(),
				saga.getState(),
				saga.getStepIndex(),
				registry.totalSteps(saga.getSagaType()),
				saga.getAttempts(),
				saga.getNextAttemptAt(),
				saga.getLastError(),
				saga.getCorrelationId(),
				saga.getCreatedAt(),
				saga.getUpdatedAt());
	}

}
