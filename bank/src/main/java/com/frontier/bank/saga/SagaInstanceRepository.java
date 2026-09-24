package com.frontier.bank.saga;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SagaInstanceRepository extends JpaRepository<SagaInstance, UUID> {

	Optional<SagaInstance> findByIdempotencyKey(String idempotencyKey);

	List<SagaInstance> findAllByOrderByCreatedAtDesc(Pageable pageable);

	List<SagaInstance> findAllByStateOrderByCreatedAtAsc(SagaState state, Pageable pageable);

	/**
	 * Sagas que precisam de avanço: em execução (ou compensando) cuja próxima
	 * tentativa já venceu. Sem lock explícito — a idempotência por passo
	 * ({@code saga_step_executions}) é o que impede execução duplicada caso duas
	 * instâncias peguem a mesma saga.
	 */
	@Query("""
			SELECT s FROM SagaInstance s
			WHERE s.state IN (com.frontier.bank.saga.SagaState.RUNNING, com.frontier.bank.saga.SagaState.COMPENSATING)
			  AND (s.nextAttemptAt IS NULL OR s.nextAttemptAt <= :now)
			ORDER BY s.createdAt ASC
			""")
	List<SagaInstance> findPending(@Param("now") Instant now, Pageable pageable);

	long countByState(SagaState state);

	long countByStateIn(java.util.Collection<SagaState> states);

}
