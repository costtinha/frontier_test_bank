package com.frontier.bank.common.projection.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.frontier.bank.balance.event.MoneyDeposited;
import com.frontier.bank.common.error.ResourceNotFoundException;
import com.frontier.bank.common.event.DomainEvent;
import com.frontier.bank.common.event.EventMessage;
import com.frontier.bank.common.event.EventMessageMapper;
import com.frontier.bank.common.event.OutboxEvent;
import com.frontier.bank.common.event.OutboxEventRepository;
import com.frontier.bank.common.projection.ProcessedEventRepository;
import com.frontier.bank.common.projection.ProjectionCheckpoint;
import com.frontier.bank.common.projection.ProjectionCheckpointRepository;
import com.frontier.bank.common.projection.ProjectionDeadLetter;
import com.frontier.bank.common.projection.ProjectionDeadLetterRepository;
import com.frontier.bank.common.projection.ProjectionHandler;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ProjectionAdminServiceTest {

	private static final String CONSUMER = "test-consumer";

	@Mock
	private ProjectionCheckpointRepository checkpointRepository;

	@Mock
	private ProcessedEventRepository processedEventRepository;

	@Mock
	private ProjectionDeadLetterRepository deadLetterRepository;

	@Mock
	private OutboxEventRepository outboxRepository;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private ProjectionAdminService service;

	@BeforeEach
	void setUp() {
		ProjectionHandler handler = new ProjectionHandler() {

			@Override
			public String consumerName() {
				return CONSUMER;
			}

			@Override
			public Set<String> supportedEventTypes() {
				return Set.of(MoneyDeposited.TYPE);
			}

			@Override
			public void apply(EventMessage event) {
				// no-op
			}
		};
		service = new ProjectionAdminService(List.of(handler), checkpointRepository, processedEventRepository,
				deadLetterRepository, outboxRepository);
	}

	@Test
	void shouldReportConsumerStatus() {
		when(checkpointRepository.findById(CONSUMER))
				.thenReturn(Optional.of(ProjectionCheckpoint.initial(CONSUMER)));
		when(processedEventRepository.countByConsumer(CONSUMER)).thenReturn(5L);
		when(outboxRepository.countAfter(any(), any())).thenReturn(2L);
		when(deadLetterRepository.countByConsumer(CONSUMER)).thenReturn(1L);

		List<ProjectionStatus> statuses = service.status();

		assertThat(statuses).hasSize(1);
		ProjectionStatus status = statuses.get(0);
		assertThat(status.consumer()).isEqualTo(CONSUMER);
		assertThat(status.processedEvents()).isEqualTo(5);
		assertThat(status.pendingEvents()).isEqualTo(2);
		assertThat(status.deadLetters()).isEqualTo(1);
	}

	@Test
	void shouldRewindCheckpointAndClearDedupeOnReplay() {
		stubStatusRepositories();

		ProjectionStatus status = service.replay(CONSUMER, null);

		verify(processedEventRepository).deleteByConsumer(CONSUMER);
		verify(checkpointRepository).save(argThat(checkpoint ->
				ProjectionCheckpoint.EPOCH.equals(checkpoint.getLastOccurredAt())
						&& ProjectionCheckpoint.NO_EVENT.equals(checkpoint.getLastEventId())));
		assertThat(status.consumer()).isEqualTo(CONSUMER);
	}

	@Test
	void shouldRewindToGivenInstantOnReplay() {
		stubStatusRepositories();
		Instant from = Instant.parse("2025-06-01T10:00:00Z");

		service.replay(CONSUMER, from);

		verify(checkpointRepository).save(argThat(checkpoint -> from.equals(checkpoint.getLastOccurredAt())));
	}

	@Test
	void shouldRewindToEventWhenRetryingDeadLetter() {
		DomainEvent<?> event = DomainEvent.of(new MoneyDeposited(UUID.randomUUID(), UUID.randomUUID(),
				UUID.randomUUID(), new BigDecimal("10.00"), new BigDecimal("110.00")));
		OutboxEvent row = OutboxEvent.from(event, objectMapper.writeValueAsString(event.payload()));
		ProjectionDeadLetter deadLetter = ProjectionDeadLetter.first(CONSUMER,
				new EventMessageMapper(objectMapper).toMessage(row), "falha simulada");

		when(deadLetterRepository.findByConsumerAndEventId(CONSUMER, event.eventId()))
				.thenReturn(Optional.of(deadLetter));
		when(outboxRepository.findByEventId(event.eventId())).thenReturn(Optional.of(row));
		stubStatusRepositories();

		service.retryDeadLetter(CONSUMER, event.eventId());

		verify(deadLetterRepository).delete(deadLetter);
		verify(checkpointRepository).save(argThat(checkpoint ->
				event.eventId().equals(checkpoint.getLastEventId())));
	}

	@Test
	void shouldRejectUnknownConsumer() {
		assertThatThrownBy(() -> service.replay("inexistente", null))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	private void stubStatusRepositories() {
		when(checkpointRepository.findById(CONSUMER))
				.thenReturn(Optional.of(ProjectionCheckpoint.initial(CONSUMER)));
		when(processedEventRepository.countByConsumer(CONSUMER)).thenReturn(0L);
		when(outboxRepository.countAfter(any(), any())).thenReturn(0L);
		when(deadLetterRepository.countByConsumer(CONSUMER)).thenReturn(0L);
	}

}
