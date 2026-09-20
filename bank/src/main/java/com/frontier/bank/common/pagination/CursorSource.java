package com.frontier.bank.common.pagination;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Fonte de dados paginável por cursor: expõe as quatro consultas do keyset
 * pagination e como extrair de cada item a chave de ordenação
 * {@code (occurred_at, id)}.
 */
public interface CursorSource<T> {

	List<T> fetchFirstPage(int size);

	List<T> fetchAfter(Instant occurredAt, UUID id, int size);

	List<T> fetchBefore(Instant occurredAt, UUID id, int size);

	List<T> fetchLastPage(int size);

	Instant occurredAtOf(T item);

	UUID idOf(T item);

}
