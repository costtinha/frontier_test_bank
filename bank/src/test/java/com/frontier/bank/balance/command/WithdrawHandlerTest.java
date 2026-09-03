package com.frontier.bank.balance.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.frontier.bank.balance.Balance;
import com.frontier.bank.balance.BalanceRepository;
import com.frontier.bank.common.error.InsufficientFundsException;
import com.frontier.bank.common.error.ResourceNotFoundException;
import com.frontier.bank.user.User;
import com.frontier.bank.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class WithdrawHandlerTest {

	@Mock
	private BalanceRepository balanceRepository;

	@Mock
	private UserRepository userRepository;

	private WithdrawHandler handler;

	private UUID userId;
	private User user;

	@BeforeEach
	void setUp() {
		handler = new WithdrawHandler(balanceRepository, userRepository);
		userId = UUID.randomUUID();
		user = new User("João", "joao@example.com", "12345678901", "hash");
		ReflectionTestUtils.setField(user, "id", userId);
	}

	@Test
	void shouldWithdrawMoney() {
		Balance balance = balanceOf(new BigDecimal("200.00"));
		when(userRepository.existsById(userId)).thenReturn(true);
		when(balanceRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(balance));
		when(balanceRepository.saveAndFlush(balance)).thenReturn(balance);

		UUID result = handler.handle(new WithdrawCommand(userId, new BigDecimal("150.00")));

		assertThat(result).isEqualTo(balance.getId());
		assertThat(balance.getAmount()).isEqualByComparingTo("50.00");
	}

	@Test
	void shouldAllowWithdrawingEntireBalance() {
		Balance balance = balanceOf(new BigDecimal("10.00"));
		when(userRepository.existsById(userId)).thenReturn(true);
		when(balanceRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(balance));
		when(balanceRepository.saveAndFlush(balance)).thenReturn(balance);

		handler.handle(new WithdrawCommand(userId, new BigDecimal("10.00")));

		assertThat(balance.getAmount()).isEqualByComparingTo("0.00");
	}

	@Test
	void shouldRejectWithdrawExceedingBalance() {
		Balance balance = balanceOf(new BigDecimal("10.00"));
		when(userRepository.existsById(userId)).thenReturn(true);
		when(balanceRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(balance));

		assertThatThrownBy(() -> handler.handle(new WithdrawCommand(userId, new BigDecimal("50.00"))))
				.isInstanceOf(InsufficientFundsException.class);
		verify(balanceRepository, never()).saveAndFlush(any());
	}

	@Test
	void shouldRejectNonPositiveAmount() {
		when(userRepository.existsById(userId)).thenReturn(true);

		assertThatThrownBy(() -> handler.handle(new WithdrawCommand(userId, BigDecimal.ZERO)))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> handler.handle(new WithdrawCommand(userId, new BigDecimal("-5.00"))))
				.isInstanceOf(IllegalArgumentException.class);
		verify(balanceRepository, never()).findByUserIdForUpdate(any());
	}

	@Test
	void shouldRejectWithdrawForUnknownUser() {
		when(userRepository.existsById(userId)).thenReturn(false);

		assertThatThrownBy(() -> handler.handle(new WithdrawCommand(userId, new BigDecimal("10.00"))))
				.isInstanceOf(ResourceNotFoundException.class);
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
