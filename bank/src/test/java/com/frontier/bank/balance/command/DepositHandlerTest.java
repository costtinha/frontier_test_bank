package com.frontier.bank.balance.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
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
import com.frontier.bank.balance.event.MoneyDeposited;
import com.frontier.bank.common.error.ResourceNotFoundException;
import com.frontier.bank.common.event.DomainEvent;
import com.frontier.bank.common.event.EventPublisher;
import com.frontier.bank.ledger.LedgerEntry;
import com.frontier.bank.ledger.LedgerEntryType;
import com.frontier.bank.ledger.LedgerRepository;
import com.frontier.bank.user.User;
import com.frontier.bank.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class DepositHandlerTest {

	@Mock
	private BalanceRepository balanceRepository;

	@Mock
	private LedgerRepository ledgerRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private EventPublisher eventPublisher;

	private DepositHandler handler;

	private UUID userId;
	private User user;

	@BeforeEach
	void setUp() {
		handler = new DepositHandler(balanceRepository, ledgerRepository, userRepository, eventPublisher);
		userId = UUID.randomUUID();
		user = new User("João", "joao@example.com", "12345678901", "hash");
		ReflectionTestUtils.setField(user, "id", userId);
	}

	@Test
	void shouldDepositMoney() {
		Balance balance = balanceOf(new BigDecimal("0.00"));
		when(userRepository.existsById(userId)).thenReturn(true);
		when(balanceRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(balance));
		when(balanceRepository.saveAndFlush(balance)).thenReturn(balance);

		UUID result = handler.handle(new DepositCommand(userId, new BigDecimal("100.50")));

		assertThat(result).isEqualTo(balance.getId());
		assertThat(balance.getAmount()).isEqualByComparingTo("100.50");
		verify(balanceRepository).findByUserIdForUpdate(userId);

		// evento de dinheiro movimentado publicado com o saldo resultante
		ArgumentCaptor<DomainEvent<?>> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
		verify(eventPublisher).publish(eventCaptor.capture());
		DomainEvent<?> event = eventCaptor.getValue();
		assertThat(event.eventType()).isEqualTo(MoneyDeposited.TYPE);
		assertThat(event.aggregateId()).isEqualTo(balance.getId());
		assertThat(event.payload()).isInstanceOfSatisfying(MoneyDeposited.class, deposited -> {
			assertThat(deposited.userId()).isEqualTo(userId);
			assertThat(deposited.amount()).isEqualByComparingTo("100.50");
			assertThat(deposited.resultingBalance()).isEqualByComparingTo("100.50");
			assertThat(deposited.transactionId()).isNotNull();
		});

		// razão e evento compartilham o mesmo transactionId (visões correlacionáveis)
		ArgumentCaptor<LedgerEntry> ledgerCaptor = ArgumentCaptor.forClass(LedgerEntry.class);
		verify(ledgerRepository).save(ledgerCaptor.capture());
		LedgerEntry entry = ledgerCaptor.getValue();
		assertThat(entry.getType()).isEqualTo(LedgerEntryType.DEPOSIT);
		assertThat(entry.getBalanceId()).isEqualTo(balance.getId());
		assertThat(entry.getBalanceBefore()).isEqualByComparingTo("0.00");
		assertThat(entry.getBalanceAfter()).isEqualByComparingTo("100.50");
		assertThat(entry.getTransactionId()).isEqualTo(depositedTransactionId(event));
	}

	private UUID depositedTransactionId(DomainEvent<?> event) {
		return ((MoneyDeposited) event.payload()).transactionId();
	}

	@Test
	void shouldCreateBalanceDefensivelyOnFirstDeposit() {
		when(userRepository.existsById(userId)).thenReturn(true);
		when(balanceRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.empty());
		when(userRepository.getReferenceById(userId)).thenReturn(user);
		// snapshot do valor no momento de cada flush (a mesma referência é mutada depois)
		List<BigDecimal> flushedAmounts = new ArrayList<>();
		when(balanceRepository.saveAndFlush(any(Balance.class))).thenAnswer(inv -> {
			Balance persisted = inv.getArgument(0);
			flushedAmounts.add(persisted.getAmount());
			return persisted;
		});

		handler.handle(new DepositCommand(userId, new BigDecimal("10.00")));

		// criação defensiva com R$ 0,00 (flush 1) e persistência pós-depósito (flush 2)
		assertThat(flushedAmounts).containsExactly(
				new BigDecimal("0.00"),
				new BigDecimal("10.00"));
	}

	@Test
	void shouldRejectNonPositiveAmount() {
		when(userRepository.existsById(userId)).thenReturn(true);

		assertThatThrownBy(() -> handler.handle(new DepositCommand(userId, BigDecimal.ZERO)))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> handler.handle(new DepositCommand(userId, new BigDecimal("-5.00"))))
				.isInstanceOf(IllegalArgumentException.class);
		verify(balanceRepository, never()).findByUserIdForUpdate(any());
		verify(eventPublisher, never()).publish(any());
	}

	@Test
	void shouldRejectDepositForUnknownUser() {
		when(userRepository.existsById(userId)).thenReturn(false);

		assertThatThrownBy(() -> handler.handle(new DepositCommand(userId, new BigDecimal("10.00"))))
				.isInstanceOf(ResourceNotFoundException.class);
		verify(eventPublisher, never()).publish(any());
	}

	private Balance balanceOf(BigDecimal amount) {
		Balance balance = new Balance(user);
		ReflectionTestUtils.setField(balance, "id", UUID.randomUUID());
		ReflectionTestUtils.setField(balance, "amount", amount);
		ReflectionTestUtils.setField(balance, "createdAt", Instant.parse("2025-06-01T10:00:00Z"));
		ReflectionTestUtils.setField(balance, "updatedAt", Instant.parse("2025-06-01T10:00:00Z"));
		return balance;
	}

}
