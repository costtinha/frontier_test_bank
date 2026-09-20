package com.frontier.bank.common.pagination;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Paginação por cursor com navegação forward/backward, genérica para qualquer
 * {@link CursorSource}.
 * <p>
 * Estratégia: busca-se um item a mais que o tamanho pedido apenas para saber se
 * existe próxima/anterior página — sem {@code OFFSET}, com custo constante e
 * estável sob inserções concorrentes. Os itens voltam sempre na ordem de
 * exibição (ascendente), independentemente da direção navegada.
 */
public final class CursorPaginator {

	private static final int MAX_PAGE_SIZE = 100;

	private CursorPaginator() {
	}

	public static <T> CursorPage<T> paginate(CursorSource<T> source, int limit, String cursor,
			CursorDirection direction) {
		int pageSize = Math.max(1, Math.min(limit, MAX_PAGE_SIZE));
		return switch (direction) {
			case FORWARD -> forward(source, pageSize, cursor);
			case BACKWARD -> backward(source, pageSize, cursor);
		};
	}

	private static <T> CursorPage<T> forward(CursorSource<T> source, int pageSize, String cursor) {
		CursorCodec.Cursor decoded = decode(cursor);
		List<T> rows;
		boolean hasPrevious;

		if (decoded == null) {
			rows = source.fetchFirstPage(pageSize + 1);
			hasPrevious = false;
		} else {
			rows = source.fetchAfter(decoded.createdAt(), decoded.id(), pageSize + 1);
			hasPrevious = true;
		}

		boolean hasNext = rows.size() > pageSize;
		List<T> items = hasNext ? rows.subList(0, pageSize) : rows;
		return toPage(source, items, hasNext, hasPrevious);
	}

	private static <T> CursorPage<T> backward(CursorSource<T> source, int pageSize, String cursor) {
		CursorCodec.Cursor decoded = decode(cursor);
		List<T> rows;
		boolean hasNext;

		if (decoded == null) {
			rows = source.fetchLastPage(pageSize + 1);
			hasNext = false;
		} else {
			rows = source.fetchBefore(decoded.createdAt(), decoded.id(), pageSize + 1);
			hasNext = true;
		}

		boolean hasPrevious = rows.size() > pageSize;
		List<T> items = hasPrevious ? rows.subList(0, pageSize) : rows;
		List<T> display = new ArrayList<>(items);
		Collections.reverse(display); // consulta foi descendente: volta à ordem de exibição
		return toPage(source, display, hasNext, hasPrevious);
	}

	private static <T> CursorPage<T> toPage(CursorSource<T> source, List<T> items, boolean hasNext,
			boolean hasPrevious) {
		String nextCursor = null;
		String previousCursor = null;

		if (hasNext && !items.isEmpty()) {
			T last = items.get(items.size() - 1);
			nextCursor = CursorCodec.encode(source.occurredAtOf(last), source.idOf(last));
		}
		if (hasPrevious && !items.isEmpty()) {
			T first = items.get(0);
			previousCursor = CursorCodec.encode(source.occurredAtOf(first), source.idOf(first));
		}

		return CursorPage.of(items, nextCursor, previousCursor, hasNext, hasPrevious);
	}

	private static CursorCodec.Cursor decode(String cursor) {
		if (cursor == null || cursor.isBlank()) {
			return null;
		}
		return CursorCodec.decode(cursor);
	}

}
