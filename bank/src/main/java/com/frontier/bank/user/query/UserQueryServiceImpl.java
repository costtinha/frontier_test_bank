package com.frontier.bank.user.query;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.frontier.bank.common.error.ResourceNotFoundException;
import com.frontier.bank.common.pagination.CursorCodec;
import com.frontier.bank.common.pagination.CursorDirection;
import com.frontier.bank.common.pagination.CursorPage;

/**
 * Implementação do lado de leitura: consultas otimistas (read-only) sobre
 * projeções, com paginação por cursor bidirecional.
 */
@Service
public class UserQueryServiceImpl implements UserQueryService {

	private static final int MAX_PAGE_SIZE = 100;

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
		int pageSize = Math.max(1, Math.min(limit, MAX_PAGE_SIZE));
		// Busca um item extra apenas para detectar próxima/anterior página.
		Pageable pageable = Pageable.ofSize(pageSize + 1);

		return switch (direction) {
			case FORWARD -> findForward(pageable, pageSize, cursor);
			case BACKWARD -> findBackward(pageable, pageSize, cursor);
		};
	}

	private CursorPage<UserSummary> findForward(Pageable pageable, int pageSize, String cursor) {
		CursorCodec.Cursor decoded = decode(cursor);
		List<UserSummary> rows;
		boolean hasPrevious;

		if (decoded == null) {
			rows = queryRepository.findFirstPage(pageable);
			hasPrevious = false;
		} else {
			rows = queryRepository.findAfter(decoded.createdAt(), decoded.id(), pageable);
			hasPrevious = true;
		}

		boolean hasNext = rows.size() > pageSize;
		List<UserSummary> items = hasNext ? rows.subList(0, pageSize) : rows;
		return toPage(items, hasNext, hasPrevious);
	}

	private CursorPage<UserSummary> findBackward(Pageable pageable, int pageSize, String cursor) {
		CursorCodec.Cursor decoded = decode(cursor);
		List<UserSummary> rows;
		boolean hasNext;

		if (decoded == null) {
			rows = queryRepository.findLastPage(pageable);
			hasNext = false;
		} else {
			rows = queryRepository.findBefore(decoded.createdAt(), decoded.id(), pageable);
			hasNext = true;
		}

		boolean hasPrevious = rows.size() > pageSize;
		List<UserSummary> items = hasPrevious ? rows.subList(0, pageSize) : rows;
		List<UserSummary> display = new ArrayList<>(items);
		java.util.Collections.reverse(display);
		return toPage(display, hasNext, hasPrevious);
	}

	private CursorCodec.Cursor decode(String cursor) {
		if (cursor == null || cursor.isBlank()) {
			return null;
		}
		return CursorCodec.decode(cursor);
	}

	private CursorPage<UserSummary> toPage(List<UserSummary> items, boolean hasNext, boolean hasPrevious) {
		String nextCursor = null;
		String previousCursor = null;

		if (hasNext && !items.isEmpty()) {
			UserSummary last = items.get(items.size() - 1);
			nextCursor = CursorCodec.encode(last.createdAt(), last.id());
		}
		if (hasPrevious && !items.isEmpty()) {
			UserSummary first = items.get(0);
			previousCursor = CursorCodec.encode(first.createdAt(), first.id());
		}

		return CursorPage.of(items, nextCursor, previousCursor, hasNext, hasPrevious);
	}

}
