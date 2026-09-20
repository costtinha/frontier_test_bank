package com.frontier.bank.common.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.frontier.bank.user.UserRole;
import com.frontier.bank.user.event.UserRegistered;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class OutboxDispatcherTest {

	private static final int BATCH_SIZE = 50;

	@Mock
	private OutboxEventRepository repository;

	@Mock
	private EventTransport transport;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private OutboxDispatcher dispatcher;

	@BeforeEach
	void setUp() {
		dispatcher = new OutboxDispatcher(repository, transport, objectMapper);
	}

	@Test
	void shouldPublishPendingEventsAndMarkThemPublished() {
		UUID userId = UUID.randomUUID();
		OutboxEvent row = outboxRow(new UserRegistered(userId, "João", "joao@example.com", UserRole.USER));
		when(repository.lockPendingBatch(BATCH_SIZE)).thenReturn(List.of(row));

		int published = dispatcher.publishPending(BATCH_SIZE);

		assertThat(published).isEqualTo(1);
		ArgumentCaptor<EventMessage> captor = ArgumentCaptor.forClass(EventMessage.class);
		verify(transport).publish(captor.capture());

		EventMessage message = captor.getValue();
		assertThat(message.eventType()).isEqualTo(UserRegistered.TYPE);
		assertThat(message.aggregateId()).isEqualTo(userId);
		assertThat(message.eventId()).isEqualTo(row.getEventId());
		assertThat(message.payload().get("email").asText()).isEqualTo("joao@example.com");

		assertThat(row.getPublishedAt()).isNotNull();
		assertThat(row.getLastError()).isNull();
		assertThat(row.getAttempts()).isEqualTo(1);
	}

	@Test
	void shouldKeepEventPendingWhenTransportFails() {
		OutboxEvent row = outboxRow(new UserRegistered(UUID.randomUUID(), "João", "joao@example.com", UserRole.USER));
		when(repository.lockPendingBatch(BATCH_SIZE)).thenReturn(List.of(row));
		doThrow(new EventTransportException("broker indisponível", null)).when(transport).publish(any());

		int published = dispatcher.publishPending(BATCH_SIZE);

		assertThat(published).isZero();
		assertThat(row.getPublishedAt()).isNull();
		assertThat(row.getAttempts()).isEqualTo(1);
		assertThat(row.getLastError()).contains("broker indisponível");
	}

	@Test
	void shouldKeepProcessingBatchWhenOneEventFails() {
		OutboxEvent failing = outboxRow(new UserRegistered(UUID.randomUUID(), "A", "a@example.com", UserRole.USER));
		OutboxEvent healthy = outboxRow(new UserRegistered(UUID.randomUUID(), "B", "b@example.com", UserRole.USER));
		when(repository.lockPendingBatch(BATCH_SIZE)).thenReturn(List.of(failing, healthy));
		doThrow(new EventTransportException("falha simulada", null)).when(transport)
				.publish(argThatMessageWithEventId(failing.getEventId()));

		int published = dispatcher.publishPending(BATCH_SIZE);

		assertThat(published).isEqualTo(1);
		assertThat(failing.getPublishedAt()).isNull();
		assertThat(healthy.getPublishedAt()).isNotNull();
	}

	@Test
	void shouldDoNothingWhenThereIsNoPendingEvent() {
		when(repository.lockPendingBatch(BATCH_SIZE)).thenReturn(List.of());

		assertThat(dispatcher.publishPending(BATCH_SIZE)).isZero();
		verifyNoInteractions(transport);
	}

	private EventMessage argThatMessageWithEventId(UUID eventId) {
		return org.mockito.ArgumentMatchers.argThat(message -> eventId.equals(message.eventId()));
	}

	private OutboxEvent outboxRow(EventPayload payload) {
		DomainEvent<?> event = DomainEvent.of(payload);
		return OutboxEvent.from(event, objectMapper.writeValueAsString(payload));
	}

}
