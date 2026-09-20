package com.frontier.bank.common.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.frontier.bank.common.context.CorrelationContext;

class DomainEventTest {

	/** Payload de exemplo, só para exercitar o envelope. */
	record Sample(UUID aggregateId) implements EventPayload {

		@Override
		public String eventType() {
			return "bank.test.Sample";
		}

		@Override
		public String aggregateType() {
			return "Test";
		}
	}

	@AfterEach
	void clearContext() {
		CorrelationContext.clear();
	}

	@Test
	void shouldBuildEnvelopeFromPayload() {
		UUID aggregateId = UUID.randomUUID();

		DomainEvent<Sample> event = DomainEvent.of(new Sample(aggregateId));

		assertThat(event.eventId()).isNotNull();
		assertThat(event.eventType()).isEqualTo("bank.test.Sample");
		assertThat(event.eventVersion()).isEqualTo(1);
		assertThat(event.aggregateType()).isEqualTo("Test");
		assertThat(event.aggregateId()).isEqualTo(aggregateId);
		assertThat(event.occurredAt()).isNotNull();
		assertThat(event.correlationId()).isNotBlank();
		assertThat(event.payload()).isEqualTo(new Sample(aggregateId));
	}

	@Test
	void shouldReuseCorrelationIdFromContext() {
		CorrelationContext.set("corr-123");

		assertThat(DomainEvent.of(new Sample(UUID.randomUUID())).correlationId()).isEqualTo("corr-123");
	}

	@Test
	void shouldGenerateDistinctCorrelationIdsWithoutContext() {
		DomainEvent<Sample> first = DomainEvent.of(new Sample(UUID.randomUUID()));
		DomainEvent<Sample> second = DomainEvent.of(new Sample(UUID.randomUUID()));

		assertThat(first.correlationId()).isNotEqualTo(second.correlationId());
	}

}
