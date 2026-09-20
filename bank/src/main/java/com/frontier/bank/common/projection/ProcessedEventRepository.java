package com.frontier.bank.common.projection;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {

	boolean existsByConsumerAndEventId(String consumer, UUID eventId);

}
