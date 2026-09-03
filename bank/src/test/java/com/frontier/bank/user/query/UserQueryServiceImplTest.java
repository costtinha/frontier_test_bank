package com.frontier.bank.user.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.frontier.bank.common.error.ResourceNotFoundException;
import com.frontier.bank.common.pagination.CursorCodec;
import com.frontier.bank.common.pagination.CursorDirection;
import com.frontier.bank.common.pagination.CursorPage;
import com.frontier.bank.user.UserRole;

@ExtendWith(MockitoExtension.class)
class UserQueryServiceImplTest {

	@Mock
	private UserQueryRepository queryRepository;

	private UserQueryServiceImpl queryService;

	@BeforeEach
	void setUp() {
		queryService = new UserQueryServiceImpl(queryRepository);
	}

	@Test
	void shouldFindUserDetailsById() {
		UUID id = UUID.randomUUID();
		Instant createdAt = Instant.parse("2025-06-01T10:00:00Z");
		UserDetails details = new UserDetails(id, "João", "joao@example.com", "12345678901",
				UserRole.USER, true, createdAt, createdAt);
		when(queryRepository.findDetailsById(id)).thenReturn(Optional.of(details));

		UserDetails result = queryService.findById(id);

		assertThat(result.id()).isEqualTo(id);
		assertThat(result.email()).isEqualTo("joao@example.com");
	}

	@Test
	void shouldThrowNotFoundWhenDetailsMissing() {
		UUID id = UUID.randomUUID();
		when(queryRepository.findDetailsById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> queryService.findById(id))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	// --- Paginação por cursor (forward) ---

	@Test
	void shouldReturnFirstPageForwardWithoutCursor() {
		List<UserSummary> rows = List.of(
				summary(UUID.randomUUID(), "2025-06-01T10:00:00Z"),
				summary(UUID.randomUUID(), "2025-06-01T10:00:01Z"),
				summary(UUID.randomUUID(), "2025-06-01T10:00:02Z"));

		when(queryRepository.findFirstPage(any(Pageable.class))).thenReturn(rows);

		CursorPage<UserSummary> page = queryService.findAll(2, null, CursorDirection.FORWARD);

		assertThat(page.items()).hasSize(2);
		assertThat(page.hasNext()).isTrue();
		assertThat(page.nextCursor()).isNotBlank();
		assertThat(page.hasPrevious()).isFalse();
		assertThat(page.previousCursor()).isNull();
	}

	@Test
	void shouldNavigateForwardUsingCursor() {
		UUID id2 = UUID.randomUUID();
		UUID id3 = UUID.randomUUID();
		Instant t1 = Instant.parse("2025-06-01T10:00:00Z");
		Instant t2 = Instant.parse("2025-06-01T10:00:01Z");
		Instant t3 = Instant.parse("2025-06-01T10:00:02Z");
		when(queryRepository.findAfter(eq(t1), eq(id2), any(Pageable.class)))
				.thenReturn(List.of(summary(id2, t2), summary(id3, t3)));

		CursorPage<UserSummary> page = queryService.findAll(5, CursorCodec.encode(t1, id2), CursorDirection.FORWARD);

		assertThat(page.items()).hasSize(2);
		assertThat(page.hasNext()).isFalse();
		assertThat(page.nextCursor()).isNull();
		assertThat(page.hasPrevious()).isTrue();
		assertThat(page.previousCursor()).isNotBlank();
	}

	@Test
	void shouldRejectInvalidCursor() {
		assertThatThrownBy(() -> queryService.findAll(5, "###invalido###", CursorDirection.FORWARD))
				.isInstanceOf(IllegalArgumentException.class);
	}

	// --- Paginação por cursor (backward) ---

	@Test
	void shouldReturnLastPageBackwardWithoutCursor() {
		UUID id1 = UUID.randomUUID();
		UUID id2 = UUID.randomUUID();
		UUID id3 = UUID.randomUUID();
		List<UserSummary> rows = List.of( // descendente: mais recentes primeiro
				summary(id3, "2025-06-01T10:00:02Z"),
				summary(id2, "2025-06-01T10:00:01Z"),
				summary(id1, "2025-06-01T10:00:00Z"));

		when(queryRepository.findLastPage(any(Pageable.class))).thenReturn(rows);

		CursorPage<UserSummary> page = queryService.findAll(2, null, CursorDirection.BACKWARD);

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
		Instant t2 = Instant.parse("2025-06-01T10:00:01Z");
		Instant t3 = Instant.parse("2025-06-01T10:00:02Z");
		when(queryRepository.findBefore(eq(t3), eq(id2), any(Pageable.class)))
				.thenReturn(List.of(summary(id2, t2)));

		CursorPage<UserSummary> page = queryService.findAll(5, CursorCodec.encode(t3, id2), CursorDirection.BACKWARD);

		assertThat(page.items()).hasSize(1);
		assertThat(page.items().get(0).id()).isEqualTo(id2);
		assertThat(page.hasNext()).isTrue();
		assertThat(page.nextCursor()).isNotBlank();
		assertThat(page.hasPrevious()).isFalse();
	}

	// --- helpers ---

	private UserSummary summary(UUID id, String createdAt) {
		return summary(id, Instant.parse(createdAt));
	}

	private UserSummary summary(UUID id, Instant createdAt) {
		return new UserSummary(id, "João", "email-" + id + "@example.com", UserRole.USER, true, createdAt);
	}

}
