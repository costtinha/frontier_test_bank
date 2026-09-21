package com.frontier.bank.transfer.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.frontier.bank.balance.Balance;
import com.frontier.bank.balance.BalanceRepository;
import com.frontier.bank.common.error.IdempotencyConflictException;
import com.frontier.bank.common.error.ResourceNotFoundException;
import com.frontier.bank.common.event.DomainEvent;
import com.frontier.bank.common.event.EventPublisher;
import com.frontier.bank.common.observability.BankMetrics;
import com.frontier.bank.ledger.LedgerEntry;
import com.frontier.bank.ledger.LedgerEntryType;
import com.frontier.bank.ledger.LedgerRepository;
import com.frontier.bank.transfer.Transfer;
import com.frontier.bank.transfer.TransferRepository;
import com.frontier.bank.transfer.TransferStatus;
import com.frontier.bank.transfer.event.TransferCompleted;
import com.frontier.bank.transfer.event.TransferCredited;
import com.frontier.bank.transfer.event.TransferDebited;
import com.frontier.bank.transfer.event.TransferFailed;
import com.frontier.bank.transfer.event.TransferInitiated;
import com.frontier.bank.user.User;
import com.frontier.bank.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class TransferMoneyHandlerTest {

	private static final String IDEMPOTENCY_KEY = "chave-123";

	@Mock
	private BalanceRepository balanceRepository;

	@Mock
	private TransferRepository transferRepository;

	@Mock
	private LedgerRepository ledgerRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private EventPublisher eventPublisher;

	@Mock
	private BankMetrics metrics;

	private TransferMoneyHandler handler;

	private UUID sourceUserId;
	private UUID targetUserId;
	private UUID sourceBalanceId;
	private UUID targetBalanceId;
	private User sourceUser;
	private User targetUser;

	@BeforeEach
	void setUp() {
		handler = new TransferMoneyHandler(balanceRepository, transferRepository, ledgerRepository,
				userRepository, eventPublisher, metrics);

		sourceUserId = UUID.randomUUID();
		targetUserId = UUID.randomUUID();
		// ids escolhidos para garantir ordem de lock previsível: origem < destino
		sourceBalanceId = UUID.fromString("00000000-0000-0000-0000-000000000001");
		targetBalanceId = UUID.fromString("00000000-0000-0000-0000-000000000002");
		sourceUser = user(sourceUserId);
		targetUser = user(targetUserId);
	}

	@Test
	void shouldTransferMoneyMovingBalancesWritingLedgerAndPublishingEvents() {
		Balance sourceBalance = balanceOf(sourceBalanceId, sourceUser, "100.00");
		Balance targetBalance = balanceOf(targetBalanceId, targetUser, "20.00");
		stubAccounts(sourceBalance, targetBalance);
		stubPersistence(sourceBalance, targetBalance);

		TransferResult result = handler.handle(new TransferMoneyCommand(sourceUserId, targetUserId,
				new BigDecimal("40.00"), IDEMPOTENCY_KEY));

		assertThat(result.isFailed()).isFalse();
		assertThat(result.status()).isEqualTo(TransferStatus.COMPLETED);
		assertThat(result.amount()).isEqualByComparingTo("40.00");

		assertThat(sourceBalance.getAmount()).isEqualByComparingTo("60.00");
		assertThat(targetBalance.getAmount()).isEqualByComparingTo("60.00");

		// razão: débito e crédito da mesma operação, com saldo antes/depois
		ArgumentCaptor<LedgerEntry> ledgerCaptor = ArgumentCaptor.forClass(LedgerEntry.class);
		verify(ledgerRepository, times(2)).save(ledgerCaptor.capture());
		List<LedgerEntry> entries = ledgerCaptor.getAllValues();

		assertThat(entries.get(0).getType()).isEqualTo(LedgerEntryType.TRANSFER_DEBIT);
		assertThat(entries.get(0).getUserId()).isEqualTo(sourceUserId);
		assertThat(entries.get(0).getBalanceBefore()).isEqualByComparingTo("100.00");
		assertThat(entries.get(0).getBalanceAfter()).isEqualByComparingTo("60.00");

		assertThat(entries.get(1).getType()).isEqualTo(LedgerEntryType.TRANSFER_CREDIT);
		assertThat(entries.get(1).getUserId()).isEqualTo(targetUserId);
		assertThat(entries.get(1).getBalanceBefore()).isEqualByComparingTo("20.00");
		assertThat(entries.get(1).getBalanceAfter()).isEqualByComparingTo("60.00");

		assertThat(entries.get(0).getTransactionId()).isEqualTo(entries.get(1).getTransactionId());
		assertThat(entries.get(0).getTransactionId()).isEqualTo(result.transferId());

		// transferência persistida + rastro de eventos do fluxo
		verify(transferRepository).saveAndFlush(any(Transfer.class));
		ArgumentCaptor<DomainEvent<?>> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
		verify(eventPublisher, times(4)).publish(eventCaptor.capture());
		assertThat(eventCaptor.getAllValues()).extracting(DomainEvent::eventType).containsExactly(
				TransferInitiated.TYPE, TransferDebited.TYPE, TransferCredited.TYPE, TransferCompleted.TYPE);
	}

	@Test
	void shouldRejectTransferWhenSourceHasInsufficientFunds() {
		Balance sourceBalance = balanceOf(sourceBalanceId, sourceUser, "10.00");
		Balance targetBalance = balanceOf(targetBalanceId, targetUser, "20.00");
		stubAccounts(sourceBalance, targetBalance);

		TransferResult result = handler.handle(new TransferMoneyCommand(sourceUserId, targetUserId,
				new BigDecimal("50.00"), IDEMPOTENCY_KEY));

		assertThat(result.isFailed()).isTrue();
		assertThat(result.failureReason()).contains("Saldo insuficiente");

		// nenhum estado muda e o fato fica registrado
		assertThat(sourceBalance.getAmount()).isEqualByComparingTo("10.00");
		assertThat(targetBalance.getAmount()).isEqualByComparingTo("20.00");
		verify(ledgerRepository, never()).save(any());
		verify(transferRepository, never()).saveAndFlush(any());

		ArgumentCaptor<DomainEvent<?>> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
		verify(eventPublisher, times(2)).publish(eventCaptor.capture());
		assertThat(eventCaptor.getAllValues()).extracting(DomainEvent::eventType).containsExactly(
				TransferInitiated.TYPE, TransferFailed.TYPE);
	}

	@Test
	void shouldReplayCompletedTransferForSameIdempotencyKey() {
		UUID transferId = UUID.randomUUID();
		Instant createdAt = Instant.parse("2025-06-01T10:00:00Z");
		Transfer stored = Transfer.completed(transferId, IDEMPOTENCY_KEY, sourceUserId, targetUserId,
				new BigDecimal("40.00"), createdAt);
		when(userRepository.existsById(any())).thenReturn(true);
		when(transferRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.of(stored));

		TransferResult result = handler.handle(new TransferMoneyCommand(sourceUserId, targetUserId,
				new BigDecimal("40.00"), IDEMPOTENCY_KEY));

		assertThat(result.transferId()).isEqualTo(transferId);
		assertThat(result.occurredAt()).isEqualTo(createdAt);
		// replay não move dinheiro nem gera novos fatos
		verify(balanceRepository, never()).findByUserIdForUpdate(any());
		verify(ledgerRepository, never()).save(any());
		verify(eventPublisher, never()).publish(any());
	}

	@Test
	void shouldRejectReuseOfIdempotencyKeyWithDifferentParameters() {
		Transfer stored = Transfer.completed(UUID.randomUUID(), IDEMPOTENCY_KEY, sourceUserId, targetUserId,
				new BigDecimal("40.00"), Instant.parse("2025-06-01T10:00:00Z"));
		when(userRepository.existsById(any())).thenReturn(true);
		when(transferRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.of(stored));

		TransferMoneyCommand command = new TransferMoneyCommand(sourceUserId, targetUserId,
				new BigDecimal("99.00"), IDEMPOTENCY_KEY);

		assertThatThrownBy(() -> handler.handle(command))
				.isInstanceOf(IdempotencyConflictException.class);
	}

	@Test
	void shouldRejectTransferToSameAccount() {
		when(userRepository.existsById(any())).thenReturn(true);

		TransferMoneyCommand command = new TransferMoneyCommand(sourceUserId, sourceUserId,
				new BigDecimal("10.00"), IDEMPOTENCY_KEY);

		assertThatThrownBy(() -> handler.handle(command))
				.isInstanceOf(IllegalArgumentException.class);
		verify(transferRepository, never()).saveAndFlush(any());
	}

	@Test
	void shouldThrowNotFoundWhenAccountDoesNotExist() {
		when(userRepository.existsById(sourceUserId)).thenReturn(true);
		when(userRepository.existsById(targetUserId)).thenReturn(true);
		when(transferRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
		when(balanceRepository.findAllByUserIdIn(any())).thenReturn(List.of(balanceOf(sourceBalanceId, sourceUser, "10.00")));

		TransferMoneyCommand command = new TransferMoneyCommand(sourceUserId, targetUserId,
				new BigDecimal("5.00"), IDEMPOTENCY_KEY);

		assertThatThrownBy(() -> handler.handle(command))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	/** Stubs comuns: usuários existem, chave é nova e as duas contas são resolvidas/travadas. */
	private void stubAccounts(Balance sourceBalance, Balance targetBalance) {
		when(userRepository.existsById(any())).thenReturn(true);
		when(transferRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
		when(balanceRepository.findAllByUserIdIn(any())).thenReturn(List.of(sourceBalance, targetBalance));
		when(balanceRepository.findByUserIdForUpdate(sourceUserId)).thenReturn(Optional.of(sourceBalance));
		when(balanceRepository.findByUserIdForUpdate(targetUserId)).thenReturn(Optional.of(targetBalance));
	}

	private void stubPersistence(Balance sourceBalance, Balance targetBalance) {
		when(balanceRepository.saveAndFlush(sourceBalance)).thenReturn(sourceBalance);
		when(balanceRepository.saveAndFlush(targetBalance)).thenReturn(targetBalance);
		when(transferRepository.saveAndFlush(any(Transfer.class))).thenAnswer(inv -> inv.getArgument(0));
	}

	private User user(UUID id) {
		User user = new User("Cliente", "cliente-" + id + "@example.com", "12345678901", "hash");
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}

	private Balance balanceOf(UUID balanceId, User user, String amount) {
		Balance balance = new Balance(user);
		ReflectionTestUtils.setField(balance, "id", balanceId);
		ReflectionTestUtils.setField(balance, "amount", new BigDecimal(amount));
		return balance;
	}

}
