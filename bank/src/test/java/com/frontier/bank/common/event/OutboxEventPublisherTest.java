package com.frontier.bank.common.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.frontier.bank.balance.event.MoneyDeposited;
import com.frontier.bank.user.UserRole;
import com.frontier.bank.user.event.UserRegistered;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class OutboxEventPublisherTest {

	@Mock
	private OutboxEventRepository repository;

	@Mock
	private ApplicationEventPublisher applicationEventPublisher;

	private OutboxEventPublisher publisher;

	@BeforeEach
	void setUp() {
		publisher = new OutboxEventPublisher(repository, new ObjectMapper(), applicationEventPublisher);
	}

	@Test
	void shouldPersistEventInOutboxAndNotifyInProcess() {
		UUID userId = UUID.randomUUID();
		DomainEvent<UserRegistered> event = DomainEvent.of(
				new UserRegistered(userId, "João", "joao@example.com", UserRole.USER));

		publisher.publish(event);

		ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
		verify(repository).save(captor.capture());
		OutboxEvent row = captor.getValue();

		assertThat(row.getEventId()).isEqualTo(event.eventId());
		assertThat(row.getEventType()).isEqualTo(UserRegistered.TYPE);
		assertThat(row.getEventVersion()).isEqualTo(1);
		assertThat(row.getAggregateType()).isEqualTo(UserRegistered.AGGREGATE);
		assertThat(row.getAggregateId()).isEqualTo(userId);
		assertThat(row.getCorrelationId()).isEqualTo(event.correlationId());
		assertThat(row.getOccurredAt()).isEqualTo(event.occurredAt());
		assertThat(row.getPayload()).contains("joao@example.com").contains("\"role\":\"USER\"");
		assertThat(row.getPublishedAt()).isNull();
		assertThat(row.getAttempts()).isZero();

		// mesmo evento entregue aos listeners in-process
		verify(applicationEventPublisher).publishEvent(event);
	}

	@Test
	void shouldSerializeMoneyAsStringInPayload() {
		UUID balanceId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();

		publisher.publish(DomainEvent.of(new MoneyDeposited(
				UUID.randomUUID(), balanceId, userId, new BigDecimal("100.50"), new BigDecimal("150.00"))));

		verify(repository).save(argThat(row -> row.getPayload().contains("\"amount\":\"100.50\"")
				&& row.getPayload().contains("\"resultingBalance\":\"150.00\"")));
	}

}
