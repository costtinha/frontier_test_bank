package com.frontier.bank.user.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.frontier.bank.common.error.ResourceNotFoundException;
import com.frontier.bank.user.User;
import com.frontier.bank.user.UserRepository;
import com.frontier.bank.user.UserRole;

@ExtendWith(MockitoExtension.class)
class UpdateUserHandlerTest {

	@Mock
	private UserRepository repository;

	@Mock
	private PasswordEncoder passwordEncoder;

	private UpdateUserHandler handler;

	@BeforeEach
	void setUp() {
		handler = new UpdateUserHandler(repository, passwordEncoder);
	}

	@Test
	void shouldUpdateOnlyProvidedFields() {
		User user = user();
		when(repository.findById(user.getId())).thenReturn(Optional.of(user));
		when(repository.findByEmail("maria@example.com")).thenReturn(Optional.empty());
		when(passwordEncoder.encode("nova-senha")).thenReturn("novo-hash");
		when(repository.saveAndFlush(user)).thenReturn(user);

		UUID result = handler.handle(new UpdateUserCommand(user.getId(), "Maria", "maria@example.com",
				"nova-senha", UserRole.ADMIN, false));

		assertThat(result).isEqualTo(user.getId());
		assertThat(user.getName()).isEqualTo("Maria");
		assertThat(user.getEmail()).isEqualTo("maria@example.com");
		assertThat(user.getPassword()).isEqualTo("novo-hash");
		assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
		assertThat(user.isActive()).isFalse();
	}

	@Test
	void shouldIgnoreNullFields() {
		User user = user();
		when(repository.findById(user.getId())).thenReturn(Optional.of(user));
		when(repository.saveAndFlush(user)).thenReturn(user);

		handler.handle(new UpdateUserCommand(user.getId(), null, null, null, null, null));

		assertThat(user.getName()).isEqualTo("João");
		assertThat(user.getEmail()).isEqualTo("joao@example.com");
		assertThat(user.isActive()).isTrue();
	}

	@Test
	void shouldAllowKeepingOwnEmail() {
		User user = user();
		when(repository.findById(user.getId())).thenReturn(Optional.of(user));
		when(repository.findByEmail("joao@example.com")).thenReturn(Optional.of(user));
		when(repository.saveAndFlush(user)).thenReturn(user);

		handler.handle(new UpdateUserCommand(user.getId(), null, "joao@example.com", null, null, null));

		assertThat(user.getEmail()).isEqualTo("joao@example.com");
	}

	@Test
	void shouldThrowNotFoundWhenUserDoesNotExist() {
		UUID id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> handler.handle(new UpdateUserCommand(id, "Maria", null, null, null, null)))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	private User user() {
		User user = new User("João", "joao@example.com", "12345678901", "hash");
		ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
		ReflectionTestUtils.setField(user, "createdAt", Instant.parse("2025-06-01T10:00:00Z"));
		return user;
	}

}
