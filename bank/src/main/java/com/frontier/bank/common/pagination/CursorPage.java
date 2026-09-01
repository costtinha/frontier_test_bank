package com.frontier.bank.common.pagination;

import java.util.List;

/**
 * Resultado de uma página em paginação por cursor.
 * <p>
 * Os itens são sempre devolvidos na ordem de exibição
 * (ascendente por {@code created_at, id}), independentemente da direção usada.
 *
 * @param items          itens da página
 * @param nextCursor     cursor para avançar (forward); {@code null} se não houver próxima página
 * @param previousCursor cursor para voltar (backward); {@code null} se não houver página anterior
 * @param hasNext        {@code true} se existem mais itens após esta página
 * @param hasPrevious    {@code true} se existem mais itens antes desta página
 */
public record CursorPage<T>(
		List<T> items,
		String nextCursor,
		String previousCursor,
		boolean hasNext,
		boolean hasPrevious) {

	public static <T> CursorPage<T> of(List<T> items, String nextCursor, String previousCursor,
			boolean hasNext, boolean hasPrevious) {
		return new CursorPage<>(items, nextCursor, previousCursor, hasNext, hasPrevious);
	}

}
