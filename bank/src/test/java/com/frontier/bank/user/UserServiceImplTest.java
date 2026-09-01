package com.frontier.bank.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.frontier.bank.common.error.DuplicateFieldException;
import com.frontier.bank.common.error.ResourceNotFoundException;
import com.frontier.bank.common.pagination.CursorCodec;
import com.frontier.bank.common.pagination.CursorDirection;
import com.frontier.bank.common.pagination.CursorPage;
import com.frontier.bank.user.dto.CreateUserRequest;
import com.frontier.bank.user.dto.UpdateUserRequest;
import com.frontier.bank.user.dto.UserResponse;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

	@Mock
	private UserRepository repository;

	@Mock
	private PasswordEncoder passwordEncoder;

	private UserServiceImpl userService;

	@BeforeEach
	void setUp() {
		userService = new UserServiceImpl(repository, passwordEncoder);
	}

	// --- CRUD ---

	@Test
	void shouldCreateUserHashingPasswordAndNormalizingEmail() {
		UUID id = UUID.randomUUID();
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

		CreateUserRequest request = new CreateUserRequest(" João ", "Joao@Example.COM", "12345678901",
				"senha-secreta", null);
		UserResponse response = userService.create(request);

		assertThat(response.id()).isEqualTo(id);
		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(repository).save(captor.capture());
		User saved = captor.getValue();
		assertThat(saved.getName()).isEqualTo("João");
		assertThat(saved.getEmail()).isEqualTo("joao@example.com");
		assertThat(saved.getCpf()).isEqualTo("12345678901");
		assertThat(saved.getPassword()).isEqualTo("hash");
		assertThat(saved.getRole()).isEqualTo(UserRole.USER);
		assertThat(saved.isActive()).isTrue();
	}

	@Test
	void shouldRejectDuplicateEmailOnCreate() {
		when(repository.findByEmail("joao@example.com")).thenReturn(Optional.of(userAt(
				Instant.parse("2025-06-01T10:00:00Z"), UUID.randomUUID())));

		CreateUserRequest request = new CreateUserRequest("João", "joao@example.com", "12345678901",
				"senha-secreta", null);

		assertThatThrownBy(() -> userService.create(request))
				.isInstanceOf(DuplicateFieldException.class)
				.hasMessageContaining("email");
		verify(repository, never()).save(any());
	}

	@Test
	void shouldFindUserById() {
		User user = userAt(Instant.parse("2025-06-01T10:00:00Z"), UUID.randomUUID());
		when(repository.findById(user.getId())).thenReturn(Optional.of(user));

		UserResponse response = userService.findById(user.getId());

		assertThat(response.id()).isEqualTo(user.getId());
		assertThat(response.email()).isEqualTo(user.getEmail());
	}

	@Test
	void shouldThrowNotFoundWhenUserDoesNotExist() {
		UUID id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userService.findById(id))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void shouldUpdateOnlyProvidedFields() {
		User user = userAt(Instant.parse("2025-06-01T10:00:00Z"), UUID.randomUUID());
		when(repository.findById(user.getId())).thenReturn(Optional.of(user));
		when(repository.findByEmail("maria@example.com")).thenReturn(Optional.empty());
		when(passwordEncoder.encode("nova-senha")).thenReturn("novo-hash");
		when(repository.saveAndFlush(user)).thenReturn(user);

		UpdateUserRequest request = new UpdateUserRequest("Maria", "maria@example.com", "nova-senha",
				UserRole.ADMIN, false);
		UserResponse response = userService.update(user.getId(), request);

		assertThat(response.name()).isEqualTo("Maria");
		assertThat(response.email()).isEqualTo("maria@example.com");
		assertThat(user.getPassword()).isEqualTo("novo-hash");
		assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
		assertThat(user.isActive()).isFalse();
	}

	@Test
	void shouldDeleteUser() {
		User user = userAt(Instant.parse("2025-06-01T10:00:00Z"), UUID.randomUUID());
		when(repository.findById(user.getId())).thenReturn(Optional.of(user));

		userService.delete(user.getId());

		verify(repository).delete(user);
	}

	@Test
	void shouldThrowNotFoundWhenDeletingMissingUser() {
		UUID id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userService.delete(id))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	// --- Paginação por cursor (forward) ---

	@Test
	void shouldReturnFirstPageForwardWithoutCursor() {
		UUID id1 = UUID.randomUUID();
		UUID id2 = UUID.randomUUID();
		UUID id3 = UUID.randomUUID();
		List<User> rows = List.of(
				userAt(Instant.parse("2025-06-01T10:00:00Z"), id1),
				userAt(Instant.parse("2025-06-01T10:00:01Z"), id2),
				userAt(Instant.parse("2025-06-01T10:00:02Z"), id3));

		when(repository.findFirstPage(any(Pageable.class))).thenReturn(rows);

		CursorPage<UserResponse> page = userService.findAll(2, null, CursorDirection.FORWARD);

		assertThat(page.items()).hasSize(2);
		assertThat(page.items().get(0).id()).isEqualTo(id1);
		assertThat(page.items().get(1).id()).isEqualTo(id2);
		assertThat(page.hasNext()).isTrue();
		assertThat(page.nextCursor()).isNotBlank();
		assertThat(page.hasPrevious()).isFalse();
		assertThat(page.previousCursor()).isNull();
	}

	@Test
	void shouldNavigateForwardUsingCursor() {
		UUID id1 = UUID.randomUUID();
		UUID id2 = UUID.randomUUID();
		UUID id3 = UUID.randomUUID();
		Instant t1 = Instant.parse("2025-06-01T10:00:00Z");
		Instant t2 = Instant.parse("2025-06-01T10:00:01Z");
		Instant t3 = Instant.parse("2025-06-01T10:00:02Z");
		List<User> rows = List.of(userAt(t2, id2), userAt(t3, id3));

		when(repository.findAfter(eq(t1), eq(id1), any(Pageable.class))).thenReturn(rows);

		CursorPage<UserResponse> page = userService.findAll(5, CursorCodec.encode(t1, id1), CursorDirection.FORWARD);

		assertThat(page.items()).hasSize(2);
		assertThat(page.hasNext()).isFalse();
		assertThat(page.nextCursor()).isNull();
		assertThat(page.hasPrevious()).isTrue();
		assertThat(page.previousCursor()).isNotBlank();
	}

	@Test
	void shouldRejectInvalidCursor() {
		assertThatThrownBy(() -> userService.findAll(5, "###invalido###", CursorDirection.FORWARD))
				.isInstanceOf(IllegalArgumentException.class);
	}

	// --- Paginação por cursor (backward) ---

	@Test
	void shouldReturnLastPageBackwardWithoutCursor() {
		UUID id1 = UUID.randomUUID();
		UUID id2 = UUID.randomUUID();
		UUID id3 = UUID.randomUUID();
		// Repositório devolve em ordem descendente (mais recentes primeiro)
		List<User> rows = List.of(
				userAt(Instant.parse("2025-06-01T10:00:02Z"), id3),
				userAt(Instant.parse("2025-06-01T10:00:01Z"), id2),
				userAt(Instant.parse("2025-06-01T10:00:00Z"), id1));

		when(repository.findLastPage(any(Pageable.class))).thenReturn(rows);

		CursorPage<UserResponse> page = userService.findAll(2, null, CursorDirection.BACKWARD);

		// Página devolvida em ordem de exibição (ascendente)
		assertThat(page.items()).hasSize(2);
		assertThat(page.items().get(0).id()).isEqualTo(id2);
		assertThat(page.items().get(1).id()).isEqualTo(id3);
		assertThat(page.hasNext()).isFalse();
		assertThat(page.hasPrevious()).isTrue();
		assertThat(page.previousCursor()).isNotBlank();
	}

	@Test
	void shouldNavigateBackwardUsingCursor() {
		UUID id2 = UUID.randomUUID();
		UUID id3 = UUID.randomUUID();
		Instant t2 = Instant.parse("2025-06-01T10:00:01Z");
		Instant t3 = Instant.parse("2025-06-01T10:00:02Z");
		List<User> rows = List.of(userAt(t2, id2));

		when(repository.findBefore(eq(t3), eq(id3), any(Pageable.class))).thenReturn(rows);

		CursorPage<UserResponse> page = userService.findAll(5, CursorCodec.encode(t3, id3), CursorDirection.BACKWARD);

		assertThat(page.items()).hasSize(1);
		assertThat(page.items().get(0).id()).isEqualTo(id2);
		assertThat(page.hasNext()).isTrue();
		assertThat(page.nextCursor()).isNotBlank();
		assertThat(page.hasPrevious()).isFalse();
	}

	// --- helpers ---

	private User userAt(Instant createdAt, UUID id) {
		User user = new User("João", "email-" + createdAt.toEpochMilli() + "@example.com",
				String.format("%011d", 10000000000L + Math.abs(id.hashCode() % 100000)), "hash");
		ReflectionTestUtils.setField(user, "id", id);
		ReflectionTestUtils.setField(user, "createdAt", createdAt);
		ReflectionTestUtils.setField(user, "updatedAt", createdAt);
		return user;
	}

}
