package com.frontier.bank.common.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.frontier.bank.common.event.OutboxEventRepository;
import com.frontier.bank.common.projection.ProjectionDeadLetterRepository;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
class BankMetricsTest {

	@Mock
	private OutboxEventRepository outboxRepository;

	@Mock
	private ProjectionDeadLetterRepository deadLetterRepository;

	private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

	private BankMetrics metrics;

	@BeforeEach
	void setUp() {
		metrics = new BankMetrics(registry, outboxRepository, deadLetterRepository);
	}

	@Test
	void shouldCountPublishedEvents() {
		metrics.eventPublished();
		metrics.eventPublished();
		metrics.eventPublishFailed();

		assertThat(registry.get("bank.events.published").counter().count()).isEqualTo(2.0);
		assertThat(registry.get("bank.events.publish.failures").counter().count()).isEqualTo(1.0);
	}

	@Test
	void shouldCountProjectionOutcomes() {
		metrics.projectionApplied();
		metrics.projectionFailed();
		metrics.projectionDeadLettered();

		assertThat(registry.get("bank.projection.applied").counter().count()).isEqualTo(1.0);
		assertThat(registry.get("bank.projection.failed").counter().count()).isEqualTo(1.0);
		assertThat(registry.get("bank.projection.dead.lettered").counter().count()).isEqualTo(1.0);
	}

	@Test
	void shouldCountTransferOutcomes() {
		metrics.transferCompleted();
		metrics.transferRejected();
		metrics.transferRejected();

		assertThat(registry.get("bank.transfers.completed").counter().count()).isEqualTo(1.0);
		assertThat(registry.get("bank.transfers.rejected").counter().count()).isEqualTo(2.0);
	}

	@Test
	void shouldExposeBacklogGauges() {
		when(outboxRepository.countByPublishedAtIsNull()).thenReturn(7L);
		when(deadLetterRepository.count()).thenReturn(2L);

		assertThat(registry.get("bank.outbox.pending").gauge().value()).isEqualTo(7.0);
		assertThat(registry.get("bank.projection.dead_letters").gauge().value()).isEqualTo(2.0);
	}

}
