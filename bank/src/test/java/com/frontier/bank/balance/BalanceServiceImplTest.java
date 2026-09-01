package com.frontier.bank.balance;

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

import com.frontier.bank.balance.dto.BalanceResponse;
import com.frontier.bank.common.error.InsufficientFundsException;
import com.frontier.bank.common.error.ResourceNotFoundException;
import com.frontier.bank.user.User;
import com.frontier.bank.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class BalanceServiceImplTest {

	@Mock
	private BalanceRepository balanceRepository;

	@Mock
	private UserRepository userRepository;

	private BalanceServiceImpl balanceService;

	private UUID userId;
	private User user;
	private Balance balance;

	@BeforeEach
	void setUp() {
		balanceService = new BalanceServiceImpl(balanceRepository, userRepository);
		userId = UUID.randomUUID();
		user = new User("João", "joao@example.com", "12345678901", "hash");
		ReflectionTestUtils.setField(user, "id", userId);
		balance = balanceOf(new BigDecimal("0.00"));
	}

	// --- Consulta ---

	@Test
	void shouldGetExistingBalance() {
		when(userRepository.existsById(userId)).thenReturn(true);
		when(balanceRepository.findByUserId(userId)).thenReturn(Optional.of(balance));

		BalanceResponse response = balanceService.getBalance(userId);

		assertThat(response.userId()).isEqualTo(userId);
		assertThat(response.amount()).isEqualByComparingTo("0.00");
	}

	@Test
	void shouldCreateZeroBalanceForLegacyUser() {
		when(userRepository.existsById(userId)).thenReturn(true);
		when(balanceRepository.findByUserId(userId)).thenReturn(Optional.empty());
		when(userRepository.getReferenceById(userId)).thenReturn(user);
		when(balanceRepository.saveAndFlush(any(Balance.class))).thenAnswer(inv -> inv.getArgument(0));

		BalanceResponse response = balanceService.getBalance(userId);

		assertThat(response.amount()).isEqualByComparingTo("0.00");
		verify(balanceRepository).saveAndFlush(any(Balance.class));
	}

	@Test
	void shouldRejectBalanceOfUnknownUser() {
		when(userRepository.existsById(userId)).thenReturn(false);

		assertThatThrownBy(() -> balanceService.getBalance(userId))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	// --- Depósito ---

	@Test
	void shouldDepositMoney() {
		when(userRepository.existsById(userId)).thenReturn(true);
		when(balanceRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(balance));
		when(balanceRepository.saveAndFlush(balance)).thenReturn(balance);

		BalanceResponse response = balanceService.deposit(userId, new BigDecimal("100.50"));

		assertThat(response.amount()).isEqualByComparingTo("100.50");
		verify(balanceRepository).findByUserIdForUpdate(userId);
	}

	@Test
	void shouldCreateBalanceForLegacyUserOnFirstDeposit() {
		when(userRepository.existsById(userId)).thenReturn(true);
		when(balanceRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.empty());
		when(userRepository.getReferenceById(userId)).thenReturn(user);
		when(balanceRepository.saveAndFlush(any(Balance.class))).thenAnswer(inv -> inv.getArgument(0));

		BalanceResponse response = balanceService.deposit(userId, new BigDecimal("10.00"));

		assertThat(response.amount()).isEqualByComparingTo("10.00");
	}

	// --- Saque ---

	@Test
	void shouldWithdrawMoney() {
		balance = balanceOf(new BigDecimal("200.00"));
		when(userRepository.existsById(userId)).thenReturn(true);
		when(balanceRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(balance));
		when(balanceRepository.saveAndFlush(balance)).thenReturn(balance);

		BalanceResponse response = balanceService.withdraw(userId, new BigDecimal("150.00"));

		assertThat(response.amount()).isEqualByComparingTo("50.00");
	}

	@Test
	void shouldAllowWithdrawingEntireBalance() {
		balance = balanceOf(new BigDecimal("10.00"));
		when(userRepository.existsById(userId)).thenReturn(true);
		when(balanceRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(balance));
		when(balanceRepository.saveAndFlush(balance)).thenReturn(balance);

		BalanceResponse response = balanceService.withdraw(userId, new BigDecimal("10.00"));

		assertThat(response.amount()).isEqualByComparingTo("0.00");
	}

	@Test
	void shouldRejectWithdrawExceedingBalance() {
		balance = balanceOf(new BigDecimal("10.00"));
		when(userRepository.existsById(userId)).thenReturn(true);
		when(balanceRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(balance));

		assertThatThrownBy(() -> balanceService.withdraw(userId, new BigDecimal("50.00")))
				.isInstanceOf(InsufficientFundsException.class);
		verify(balanceRepository, never()).saveAndFlush(any());
	}

	@Test
	void shouldRejectNonPositiveAmount() {
		when(userRepository.existsById(userId)).thenReturn(true);

		assertThatThrownBy(() -> balanceService.deposit(userId, BigDecimal.ZERO))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> balanceService.deposit(userId, new BigDecimal("-5.00")))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> balanceService.withdraw(userId, BigDecimal.ZERO))
				.isInstanceOf(IllegalArgumentException.class);
		verify(balanceRepository, never()).findByUserIdForUpdate(any());
	}

	@Test
	void shouldRejectOperationForUnknownUser() {
		when(userRepository.existsById(userId)).thenReturn(false);

		assertThatThrownBy(() -> balanceService.deposit(userId, new BigDecimal("10.00")))
				.isInstanceOf(ResourceNotFoundException.class);
		assertThatThrownBy(() -> balanceService.withdraw(userId, new BigDecimal("10.00")))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	// --- helpers ---

	private Balance balanceOf(BigDecimal amount) {
		Balance b = new Balance(user);
		ReflectionTestUtils.setField(b, "id", UUID.randomUUID());
		ReflectionTestUtils.setField(b, "amount", amount);
		ReflectionTestUtils.setField(b, "createdAt", Instant.parse("2025-06-01T10:00:00Z"));
		ReflectionTestUtils.setField(b, "updatedAt", Instant.parse("2025-06-01T10:00:00Z"));
		return b;
	}

}
