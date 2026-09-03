package com.frontier.bank.balance.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import com.frontier.bank.common.error.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class BalanceQueryServiceImplTest {

	@Mock
	private BalanceQueryRepository queryRepository;

	private BalanceQueryServiceImpl queryService;

	@BeforeEach
	void setUp() {
		queryService = new BalanceQueryServiceImpl(queryRepository);
	}

	@Test
	void shouldReturnBalanceSnapshot() {
		UUID userId = UUID.randomUUID();
		Instant now = Instant.parse("2025-06-01T10:00:00Z");
		BalanceSnapshot snapshot = new BalanceSnapshot(UUID.randomUUID(), userId,
				new BigDecimal("123.45"), now, now);
		when(queryRepository.findSnapshotByUserId(userId)).thenReturn(Optional.of(snapshot));

		BalanceSnapshot result = queryService.getBalance(userId);

		assertThat(result.userId()).isEqualTo(userId);
		assertThat(result.amount()).isEqualByComparingTo("123.45");
	}

	@Test
	void shouldThrowNotFoundWhenBalanceIsMissing() {
		UUID userId = UUID.randomUUID();
		when(queryRepository.findSnapshotByUserId(userId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> queryService.getBalance(userId))
				.isInstanceOf(ResourceNotFoundException.class);
	}

}
