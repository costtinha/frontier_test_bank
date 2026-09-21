package com.frontier.bank.common.projection;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {

	boolean existsByConsumerAndEventId(String consumer, UUID eventId);

	long countByConsumer(String consumer);

	/** Limpa o dedupe de um consumidor — base do replay. */
	@Modifying
	@Query("DELETE FROM ProcessedEvent p WHERE p.consumer = :consumer")
	void deleteByConsumer(@Param("consumer") String consumer);

}
