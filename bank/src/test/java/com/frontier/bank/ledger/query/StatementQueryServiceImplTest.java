package com.frontier.bank.ledger.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.frontier.bank.common.error.ResourceNotFoundException;
import com.frontier.bank.common.pagination.CursorDirection;
import com.frontier.bank.common.pagination.CursorPage;
import com.frontier.bank.ledger.LedgerEntryType;
import com.frontier.bank.user.query.UserQueryRepository;

@ExtendWith(MockitoExtension.class)
class StatementQueryServiceImplTest {

	@Mock
	private LedgerQueryRepository ledgerRepository;

	@Mock
	private UserQueryRepository userQueryRepository;

	private StatementQueryServiceImpl queryService;

	private UUID userId;

	@BeforeEach
	void setUp() {
		queryService = new StatementQueryServiceImpl(ledgerRepository, userQueryRepository);
		userId = UUID.randomUUID();
	}

	@Test
	void shouldReturnFirstPageOfStatement() {
		when(userQueryRepository.existsById(userId)).thenReturn(true);
		when(ledgerRepository.findFirstPage(eq(userId), any(Pageable.class))).thenReturn(List.of(
				entry("2025-06-01T10:00:00Z"),
				entry("2025-06-01T10:00:01Z"),
				entry("2025-06-01T10:00:02Z")));

		CursorPage<StatementEntry> page = queryService.findStatement(userId, 2, null, CursorDirection.FORWARD);

		assertThat(page.items()).hasSize(2);
		assertThat(page.items().get(0).occurredAt()).isEqualTo(Instant.parse("2025-06-01T10:00:00Z"));
		assertThat(page.hasNext()).isTrue();
		assertThat(page.nextCursor()).isNotBlank();
		assertThat(page.hasPrevious()).isFalse();
	}

	@Test
	void shouldReturnLastPageBackwardInDisplayOrder() {
		when(userQueryRepository.existsById(userId)).thenReturn(true);
		// repositório devolve em ordem descendente (mais recentes primeiro)
		when(ledgerRepository.findLastPage(eq(userId), any(Pageable.class))).thenReturn(List.of(
				entry("2025-06-01T10:00:02Z"),
				entry("2025-06-01T10:00:01Z"),
				entry("2025-06-01T10:00:00Z")));

		CursorPage<StatementEntry> page = queryService.findStatement(userId, 2, null, CursorDirection.BACKWARD);

		assertThat(page.items()).extracting(StatementEntry::occurredAt).containsExactly(
				Instant.parse("2025-06-01T10:00:01Z"),
				Instant.parse("2025-06-01T10:00:02Z"));
		assertThat(page.hasPrevious()).isTrue();
		assertThat(page.hasNext()).isFalse();
	}

	@Test
	void shouldThrowNotFoundWhenUserDoesNotExist() {
		when(userQueryRepository.existsById(userId)).thenReturn(false);

		assertThatThrownBy(() -> queryService.findStatement(userId, 10, null, CursorDirection.FORWARD))
				.isInstanceOf(ResourceNotFoundException.class);
		verifyNoInteractions(ledgerRepository);
	}

	private StatementEntry entry(String occurredAt) {
		return new StatementEntry(UUID.randomUUID(), UUID.randomUUID(), LedgerEntryType.DEPOSIT,
				new BigDecimal("100.00"), new BigDecimal("100.00"), Instant.parse(occurredAt));
	}

}
