package com.frontier.bank.saga;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SagaStepExecutionRepository extends JpaRepository<SagaStepExecution, UUID> {

	boolean existsBySagaIdAndStepName(UUID sagaId, String stepName);

}
