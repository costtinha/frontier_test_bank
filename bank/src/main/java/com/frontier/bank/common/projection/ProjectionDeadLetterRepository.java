package com.frontier.bank.common.projection;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectionDeadLetterRepository extends JpaRepository<ProjectionDeadLetter, UUID> {

	Optional<ProjectionDeadLetter> findByConsumerAndEventId(String consumer, UUID eventId);

	void deleteByConsumerAndEventId(String consumer, UUID eventId);

}
