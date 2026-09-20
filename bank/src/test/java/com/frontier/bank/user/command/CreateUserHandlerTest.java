package com.frontier.bank.user.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.frontier.bank.balance.Balance;
import com.frontier.bank.balance.BalanceRepository;
import com.frontier.bank.balance.event.BalanceOpened;
import com.frontier.bank.common.error.DuplicateFieldException;
import com.frontier.bank.common.event.DomainEvent;
import com.frontier.bank.common.event.EventPublisher;
import com.frontier.bank.user.User;
import com.frontier.bank.user.UserRepository;
import com.frontier.bank.user.UserRole;
import com.frontier.bank.user.event.UserRegistered;

@ExtendWith(MockitoExtension.class)
class CreateUserHandlerTest {

	@Mock
	private UserRepository repository;

	@Mock
	private BalanceRepository balanceRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private EventPublisher eventPublisher;

	private CreateUserHandler handler;

	@BeforeEach
	void setUp() {
		handler = new CreateUserHandler(repository, balanceRepository, passwordEncoder, eventPublisher);
	}

	@Test
	void shouldCreateUserHashingPasswordAndNormalizingEmail() {
		UUID id = UUID.randomUUID();
		UUID balanceId = UUID.randomUUID();
		Instant now = Instant.parse("2025-06-01T10:00:00Z");

		when(passwordEncoder.encode("senha-secreta")).thenReturn("hash");
		when(repository.findByEmail("joao@example.com")).thenReturn(Optional.empty());
		when(repository.findByCpf("12345678901")).thenReturn(Optional.empty());
		when(repository.save(any(User.class))).thenAnswer(inv -> {
			User saved = inv.getArgument(0);
			ReflectionTestUtils.setField(saved, "id", id);
			ReflectionTestUtils.setField(saved, "createdAt", now);
			ReflectionTestUtils.setField(saved, "updatedAt", now);
			return saved;
		});
		when(balanceRepository.save(any(Balance.class))).thenAnswer(inv -> {
			Balance balance = inv.getArgument(0);
			ReflectionTestUtils.setField(balance, "id", balanceId);
			return balance;
		});

		UUID result = handler.handle(new CreateUserCommand(" João ", "Joao@Example.COM",
				"12345678901", "senha-secreta", null));

		assertThat(result).isEqualTo(id);
		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(repository).save(captor.capture());
		User saved = captor.getValue();
		assertThat(saved.getName()).isEqualTo("João");
		assertThat(saved.getEmail()).isEqualTo("joao@example.com");
		assertThat(saved.getPassword()).isEqualTo("hash");
		assertThat(saved.getRole()).isEqualTo(UserRole.USER);
		assertThat(saved.isActive()).isTrue();

		// Invariante: cliente nasce com saldo R$ 0,00 na mesma transação
		ArgumentCaptor<Balance> balanceCaptor = ArgumentCaptor.forClass(Balance.class);
		verify(balanceRepository).save(balanceCaptor.capture());
		assertThat(balanceCaptor.getValue().getUser()).isSameAs(saved);
		assertThat(balanceCaptor.getValue().getAmount()).isEqualByComparingTo("0.00");

		// Dois eventos publicados na mesma transação: cadastro e abertura de conta
		ArgumentCaptor<DomainEvent<?>> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
		verify(eventPublisher, times(2)).publish(eventCaptor.capture());
		List<DomainEvent<?>> events = eventCaptor.getAllValues();

		DomainEvent<?> userEvent = events.get(0);
		assertThat(userEvent.eventType()).isEqualTo(UserRegistered.TYPE);
		assertThat(userEvent.aggregateType()).isEqualTo(UserRegistered.AGGREGATE);
		assertThat(userEvent.aggregateId()).isEqualTo(id);
		assertThat(userEvent.payload()).isInstanceOfSatisfying(UserRegistered.class, registered -> {
			assertThat(registered.userId()).isEqualTo(id);
			assertThat(registered.email()).isEqualTo("joao@example.com");
			assertThat(registered.role()).isEqualTo(UserRole.USER);
		});

		DomainEvent<?> balanceEvent = events.get(1);
		assertThat(balanceEvent.eventType()).isEqualTo(BalanceOpened.TYPE);
		assertThat(balanceEvent.aggregateId()).isEqualTo(balanceId);
		assertThat(balanceEvent.payload()).isInstanceOfSatisfying(BalanceOpened.class, opened -> {
			assertThat(opened.balanceId()).isEqualTo(balanceId);
			assertThat(opened.userId()).isEqualTo(id);
			assertThat(opened.amount()).isEqualByComparingTo("0.00");
		});
	}

	@Test
	void shouldRejectDuplicateEmail() {
		when(repository.findByEmail("joao@example.com")).thenReturn(Optional.of(user("joao@example.com")));

		assertThatThrownBy(() -> handler.handle(new CreateUserCommand("João", "joao@example.com",
				"12345678901", "senha-secreta", null)))
				.isInstanceOf(DuplicateFieldException.class)
				.hasMessageContaining("email");
		verify(repository, never()).save(any());
		verify(balanceRepository, never()).save(any());
		verify(eventPublisher, never()).publish(any());
	}

	@Test
	void shouldRejectDuplicateCpf() {
		when(repository.findByEmail("maria@example.com")).thenReturn(Optional.empty());
		when(repository.findByCpf("12345678901")).thenReturn(Optional.of(user("outro@example.com")));

		assertThatThrownBy(() -> handler.handle(new CreateUserCommand("Maria", "maria@example.com",
				"12345678901", "senha-secreta", null)))
				.isInstanceOf(DuplicateFieldException.class)
				.hasMessageContaining("cpf");
		verify(repository, never()).save(any());
	}

	private User user(String email) {
		User user = new User("Existente", email, "99999999999", "hash");
		ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
		return user;
	}

}
