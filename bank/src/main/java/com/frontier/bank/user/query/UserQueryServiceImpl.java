package com.frontier.bank.user.query;

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

/**
 * Implementação do lado de leitura: consultas otimistas (read-only) sobre
 * projeções. A paginação por cursor bidirecional é delegada ao
 * {@link CursorPaginator}, compartilhado com as demais listagens da API.
 */
@Service
public class UserQueryServiceImpl implements UserQueryService {

	private final UserQueryRepository queryRepository;

	public UserQueryServiceImpl(UserQueryRepository queryRepository) {
		this.queryRepository = queryRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails findById(UUID id) {
		return queryRepository.findDetailsById(id)
				.orElseThrow(() -> new ResourceNotFoundException("usuário", id));
	}

	@Override
	@Transactional(readOnly = true)
	public CursorPage<UserSummary> findAll(int limit, String cursor, CursorDirection direction) {
		return CursorPaginator.paginate(new UserSummarySource(), limit, cursor, direction);
	}

	/** Adapta o repositório de leitura ao contrato genérico de paginação. */
	private final class UserSummarySource implements CursorSource<UserSummary> {

		@Override
		public List<UserSummary> fetchFirstPage(int size) {
			return queryRepository.findFirstPage(Pageable.ofSize(size));
		}

		@Override
		public List<UserSummary> fetchAfter(Instant occurredAt, UUID id, int size) {
			return queryRepository.findAfter(occurredAt, id, Pageable.ofSize(size));
		}

		@Override
		public List<UserSummary> fetchBefore(Instant occurredAt, UUID id, int size) {
			return queryRepository.findBefore(occurredAt, id, Pageable.ofSize(size));
		}

		@Override
		public List<UserSummary> fetchLastPage(int size) {
			return queryRepository.findLastPage(Pageable.ofSize(size));
		}

		@Override
		public Instant occurredAtOf(UserSummary item) {
			return item.createdAt();
		}

		@Override
		public UUID idOf(UserSummary item) {
			return item.id();
		}
	}

}
