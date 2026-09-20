package com.frontier.bank.ledger.query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.frontier.bank.common.error.ResourceNotFoundException;
import com.frontier.bank.common.pagination.CursorDirection;
import com.frontier.bank.common.pagination.CursorPage;
import com.frontier.bank.common.pagination.CursorPaginator;
import com.frontier.bank.common.pagination.CursorSource;
import com.frontier.bank.user.query.UserQueryRepository;

/**
 * Leitura do extrato: paginação por cursor sobre o razão, com o mesmo contrato
 * (forward/backward) usado nas demais listagens da API.
 */
@Service
public class StatementQueryServiceImpl implements StatementQueryService {

	private final LedgerQueryRepository ledgerRepository;
	private final UserQueryRepository userQueryRepository;

	public StatementQueryServiceImpl(LedgerQueryRepository ledgerRepository, UserQueryRepository userQueryRepository) {
		this.ledgerRepository = ledgerRepository;
		this.userQueryRepository = userQueryRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public CursorPage<StatementEntry> findStatement(UUID userId, int limit, String cursor,
			CursorDirection direction) {
		if (!userQueryRepository.existsById(userId)) {
			throw new ResourceNotFoundException("usuário", userId);
		}
		return CursorPaginator.paginate(new StatementSource(userId), limit, cursor, direction);
	}

	/** Adapta o repositório do razão ao contrato genérico de paginação. */
	private final class StatementSource implements CursorSource<StatementEntry> {

		private final UUID userId;

		private StatementSource(UUID userId) {
			this.userId = userId;
		}

		@Override
		public List<StatementEntry> fetchFirstPage(int size) {
			return ledgerRepository.findFirstPage(userId, Pageable.ofSize(size));
		}

		@Override
		public List<StatementEntry> fetchAfter(Instant occurredAt, UUID id, int size) {
			return ledgerRepository.findAfter(userId, occurredAt, id, Pageable.ofSize(size));
		}

		@Override
		public List<StatementEntry> fetchBefore(Instant occurredAt, UUID id, int size) {
			return ledgerRepository.findBefore(userId, occurredAt, id, Pageable.ofSize(size));
		}

		@Override
		public List<StatementEntry> fetchLastPage(int size) {
			return ledgerRepository.findLastPage(userId, Pageable.ofSize(size));
		}

		@Override
		public Instant occurredAtOf(StatementEntry item) {
			return item.occurredAt();
		}

		@Override
		public UUID idOf(StatementEntry item) {
			return item.id();
		}
	}

}
