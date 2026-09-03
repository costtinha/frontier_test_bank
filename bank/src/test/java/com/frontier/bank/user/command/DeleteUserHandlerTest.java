package com.frontier.bank.user.command;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.frontier.bank.common.error.ResourceNotFoundException;
import com.frontier.bank.user.User;
import com.frontier.bank.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class DeleteUserHandlerTest {

	@Mock
	private UserRepository repository;

	private DeleteUserHandler handler;

	@BeforeEach
	void setUp() {
		handler = new DeleteUserHandler(repository);
	}

	@Test
	void shouldDeleteExistingUser() {
		User user = new User("João", "joao@example.com", "12345678901", "hash");
		ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
		when(repository.findById(user.getId())).thenReturn(Optional.of(user));

		handler.handle(new DeleteUserCommand(user.getId()));

		verify(repository).delete(user);
	}

	@Test
	void shouldThrowNotFoundWhenUserDoesNotExist() {
		UUID id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> handler.handle(new DeleteUserCommand(id)))
				.isInstanceOf(ResourceNotFoundException.class);
	}

}
