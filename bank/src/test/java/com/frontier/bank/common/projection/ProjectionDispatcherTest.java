package com.frontier.bank.common.projection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
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
import com.frontier.bank.common.event.DomainEvent;
import com.frontier.bank.common.event.EventMessage;
import com.frontier.bank.common.event.EventMessageMapper;
import com.frontier.bank.common.event.EventPayload;
import com.frontier.bank.common.event.OutboxEvent;
import com.frontier.bank.common.event.OutboxEventRepository;
import com.frontier.bank.user.UserRole;
import com.frontier.bank.user.event.UserRegistered;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ProjectionDispatcherTest {

	private static final String CONSUMER = "test-consumer";
	private static final int BATCH_SIZE = 10;
	private static final int MAX_ATTEMPTS = 3;

	@Mock
	private OutboxEventRepository outboxRepository;

	@Mock
	private ProjectionCheckpointRepository checkpointRepository;

	@Mock
	private ProcessedEventRepository processedEventRepository;

	@Mock
	private ProjectionDeadLetterRepository deadLetterRepository;

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final EventMessageMapper messageMapper = new EventMessageMapper(objectMapper);
	private final List<EventMessage> projected = new ArrayList<>();
	private final Set<UUID> failing = new HashSet<>();

	/** Consumidor de teste: aplica MoneyDeposited e falha sob demanda. */
	private final ProjectionHandler handler = new ProjectionHandler() {

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
			if (failing.contains(event.eventId())) {
				throw new IllegalStateException("falha simulada");
			}
			projected.add(event);
		}
	};

	private ProjectionDispatcher dispatcher;

	@BeforeEach
	void setUp() {
		ProjectionApplier applier = new ProjectionApplier(checkpointRepository, processedEventRepository,
				deadLetterRepository);
		dispatcher = new ProjectionDispatcher(List.of(handler), outboxRepository, messageMapper, applier,
				checkpointRepository, deadLetterRepository, MAX_ATTEMPTS);
	}

	@Test
	void shouldProjectSupportedEventAndAdvanceCheckpoint() {
		OutboxEvent row = outboxRow(moneyDeposited());
		stubCheckpoint();
		when(outboxRepository.findAfter(any(), any(), eq(BATCH_SIZE))).thenReturn(List.of(row));
		when(processedEventRepository.existsByConsumerAndEventId(CONSUMER, row.getEventId())).thenReturn(false);
		when(deadLetterRepository.findByConsumerAndEventId(CONSUMER, row.getEventId())).thenReturn(Optional.empty());

		int count = dispatcher.processPending(BATCH_SIZE);

		assertThat(count).isEqualTo(1);
		assertThat(projected).hasSize(1);
		assertCheckpointAdvancedTo(row);
	}

	@Test
	void shouldNotReprojectAlreadyProcessedEvent() {
		OutboxEvent row = outboxRow(moneyDeposited());
		stubCheckpoint();
		when(outboxRepository.findAfter(any(), any(), eq(BATCH_SIZE))).thenReturn(List.of(row));
		when(processedEventRepository.existsByConsumerAndEventId(CONSUMER, row.getEventId())).thenReturn(true);
		when(deadLetterRepository.findByConsumerAndEventId(CONSUMER, row.getEventId())).thenReturn(Optional.empty());

		int count = dispatcher.processPending(BATCH_SIZE);

		// dedupe: evento já aplicado não é reprojetado, mas a posição avança
		assertThat(count).isZero();
		assertThat(projected).isEmpty();
		assertCheckpointAdvancedTo(row);
	}

	@Test
	void shouldSkipEventTypeNotSupportedByConsumer() {
		OutboxEvent row = outboxRow(new UserRegistered(UUID.randomUUID(), "João", "joao@example.com", UserRole.USER));
		stubCheckpoint();
		when(outboxRepository.findAfter(any(), any(), eq(BATCH_SIZE))).thenReturn(List.of(row));

		int count = dispatcher.processPending(BATCH_SIZE);

		assertThat(count).isZero();
		assertCheckpointAdvancedTo(row);
		verifyNoInteractions(processedEventRepository);
	}

	@Test
	void shouldRecordFailureInDeadLetterAndKeepPosition() {
		OutboxEvent failingRow = outboxRow(moneyDeposited());
		OutboxEvent nextRow = outboxRow(moneyDeposited());
		failing.add(failingRow.getEventId());

		stubCheckpoint();
		when(outboxRepository.findAfter(any(), any(), eq(BATCH_SIZE))).thenReturn(List.of(failingRow, nextRow));
		when(processedEventRepository.existsByConsumerAndEventId(CONSUMER, failingRow.getEventId())).thenReturn(false);
		when(deadLetterRepository.findByConsumerAndEventId(CONSUMER, failingRow.getEventId()))
				.thenReturn(Optional.empty());

		int count = dispatcher.processPending(BATCH_SIZE);

		assertThat(count).isZero();
		assertThat(projected).isEmpty(); // lote parou no evento com falha

		verify(deadLetterRepository).save(argThat(deadLetter -> deadLetter.getAttempts() == 1
				&& "falha simulada".equals(deadLetter.getError())
				&& failingRow.getEventId().equals(deadLetter.getEventId())));

		// posição NÃO avança: o evento é reprocessado no próximo ciclo
		verify(checkpointRepository, never()).save(any());
	}

	@Test
	void shouldSkipExhaustedEventWithoutBlockingStream() {
		OutboxEvent row = outboxRow(moneyDeposited());
		ProjectionDeadLetter exhausted = ProjectionDeadLetter.first(CONSUMER, messageMapper.toMessage(row), "erro");
		for (int attempt = 1; attempt < MAX_ATTEMPTS; attempt++) {
			exhausted.failedAgain("erro");
		}

		stubCheckpoint();
		when(outboxRepository.findAfter(any(), any(), eq(BATCH_SIZE))).thenReturn(List.of(row));
		when(deadLetterRepository.findByConsumerAndEventId(CONSUMER, row.getEventId()))
				.thenReturn(Optional.of(exhausted));

		int count = dispatcher.processPending(BATCH_SIZE);

		assertThat(count).isZero();
		assertThat(projected).isEmpty();
		// tentativas esgotadas: pula o evento (evidência fica na dead letter)
		assertCheckpointAdvancedTo(row);
		verifyNoInteractions(processedEventRepository);
	}

	private void stubCheckpoint() {
		when(checkpointRepository.findById(CONSUMER))
				.thenReturn(Optional.of(ProjectionCheckpoint.initial(CONSUMER)));
	}

	private void assertCheckpointAdvancedTo(OutboxEvent row) {
		verify(checkpointRepository, atLeastOnce())
				.save(argThat(checkpoint -> row.getEventId().equals(checkpoint.getLastEventId())));
	}

	private MoneyDeposited moneyDeposited() {
		return new MoneyDeposited(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
				new BigDecimal("10.00"), new BigDecimal("110.00"));
	}

	private OutboxEvent outboxRow(EventPayload payload) {
		DomainEvent<?> event = DomainEvent.of(payload);
		return OutboxEvent.from(event, objectMapper.writeValueAsString(payload));
	}

}
