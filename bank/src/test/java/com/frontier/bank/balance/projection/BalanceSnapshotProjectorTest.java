package com.frontier.bank.balance.projection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.frontier.bank.balance.event.BalanceOpened;
import com.frontier.bank.balance.event.MoneyDeposited;
import com.frontier.bank.balance.event.MoneyWithdrawn;
import com.frontier.bank.balance.query.BalanceSnapshotView;
import com.frontier.bank.balance.query.BalanceSnapshotViewRepository;
import com.frontier.bank.common.event.DomainEvent;
import com.frontier.bank.common.event.EventMessage;
import com.frontier.bank.common.event.EventMessageMapper;
import com.frontier.bank.common.event.EventPayload;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class BalanceSnapshotProjectorTest {

	@Mock
	private BalanceSnapshotViewRepository repository;

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final EventMessageMapper messageMapper = new EventMessageMapper(objectMapper);

	private BalanceSnapshotProjector projector;

	@BeforeEach
	void setUp() {
		projector = new BalanceSnapshotProjector(repository, objectMapper);
	}

	@Test
	void shouldSupportAccountEventsOnly() {
		assertThat(projector.consumerName()).isEqualTo(BalanceSnapshotProjector.CONSUMER);
		assertThat(projector.supportedEventTypes()).containsExactlyInAnyOrder(
				BalanceOpened.TYPE, MoneyDeposited.TYPE, MoneyWithdrawn.TYPE);
	}

	@Test
	void shouldOpenSnapshotWhenBalanceIsOpened() {
		UUID balanceId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		when(repository.findByUserId(userId)).thenReturn(Optional.empty());

		projector.apply(message(new BalanceOpened(balanceId, userId, new BigDecimal("0.00"))));

		ArgumentCaptor<BalanceSnapshotView> captor = ArgumentCaptor.forClass(BalanceSnapshotView.class);
		verify(repository).save(captor.capture());
		BalanceSnapshotView view = captor.getValue();
		assertThat(view.getBalanceId()).isEqualTo(balanceId);
		assertThat(view.getUserId()).isEqualTo(userId);
		assertThat(view.getAmount()).isEqualByComparingTo("0.00");
	}

	@Test
	void shouldApplyDepositUsingResultingBalance() {
		UUID balanceId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		Instant openedAt = Instant.parse("2025-06-01T10:00:00Z");
		BalanceSnapshotView view = BalanceSnapshotView.open(balanceId, userId,
				new BigDecimal("0.00"), openedAt, UUID.randomUUID());
		when(repository.findByUserId(userId)).thenReturn(Optional.of(view));

		EventMessage event = message(new MoneyDeposited(UUID.randomUUID(), balanceId, userId,
				new BigDecimal("30.00"), new BigDecimal("100.50")), openedAt.plusSeconds(60));

		projector.apply(event);

		// saldo absoluto do evento (100,50), não a soma do valor movimentado (30,00)
		assertThat(view.getAmount()).isEqualByComparingTo("100.50");
		assertThat(view.getLastEventId()).isEqualTo(event.eventId());
		assertThat(view.getUpdatedAt()).isEqualTo(event.occurredAt());
		verify(repository).save(view);
	}

	@Test
	void shouldApplyWithdrawal() {
		UUID balanceId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		Instant openedAt = Instant.parse("2025-06-01T10:00:00Z");
		BalanceSnapshotView view = BalanceSnapshotView.open(balanceId, userId,
				new BigDecimal("100.00"), openedAt, UUID.randomUUID());
		when(repository.findByUserId(userId)).thenReturn(Optional.of(view));

		projector.apply(message(new MoneyWithdrawn(UUID.randomUUID(), balanceId, userId,
				new BigDecimal("100.00"), new BigDecimal("0.00")), openedAt.plusSeconds(60)));

		assertThat(view.getAmount()).isEqualByComparingTo("0.00");
		verify(repository).save(view);
	}

	@Test
	void shouldIgnoreOutOfOrderEvent() {
		UUID balanceId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		Instant newest = Instant.parse("2025-06-01T12:00:00Z");
		BalanceSnapshotView view = BalanceSnapshotView.open(balanceId, userId,
				new BigDecimal("100.00"), newest, UUID.randomUUID());
		when(repository.findByUserId(userId)).thenReturn(Optional.of(view));

		// evento antigo chegando depois (reprocessamento/reordenação)
		projector.apply(message(new MoneyWithdrawn(UUID.randomUUID(), balanceId, userId,
				new BigDecimal("10.00"), new BigDecimal("90.00")), newest.minusSeconds(3600)));

		assertThat(view.getAmount()).isEqualByComparingTo("100.00");
		verify(repository, never()).save(any());
	}

	@Test
	void shouldCreateSnapshotWhenMovementArrivesWithoutOpenedEvent() {
		UUID balanceId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		when(repository.findByUserId(userId)).thenReturn(Optional.empty());

		projector.apply(message(new MoneyDeposited(UUID.randomUUID(), balanceId, userId,
				new BigDecimal("25.00"), new BigDecimal("25.00"))));

		ArgumentCaptor<BalanceSnapshotView> captor = ArgumentCaptor.forClass(BalanceSnapshotView.class);
		verify(repository).save(captor.capture());
		assertThat(captor.getValue().getBalanceId()).isEqualTo(balanceId);
		assertThat(captor.getValue().getAmount()).isEqualByComparingTo("25.00");
	}

	private EventMessage message(EventPayload payload) {
		return message(payload, Instant.now());
	}

	private EventMessage message(EventPayload payload, Instant occurredAt) {
		DomainEvent<?> event = DomainEvent.of(payload);
		return new EventMessage(event.eventId(), event.eventType(), event.eventVersion(), event.aggregateType(),
				event.aggregateId(), occurredAt, event.correlationId(), event.causationId(), event.actorId(),
				objectMapper.valueToTree(payload));
	}

}
